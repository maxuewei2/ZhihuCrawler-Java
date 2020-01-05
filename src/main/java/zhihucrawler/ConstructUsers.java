package zhihucrawler;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

class User {
    public String userID;
    public String info;
    public List<String> followers;
    private int followersICount = 0;
    public List<String> followees;
    private int followeesICount = 0;
    public String topics;
    private int topicsICount = 0;
    public String questions;
    private int questionsICount = 0;
    private Instant lastUpdate;
    private ReentrantLock lock = new ReentrantLock();
    boolean isRemoved=false;

    //private static Pattern pattern = Pattern.compile("\"url_token\"\\s*:\\s*\"([^\"]+)\"");
    User(boolean isRemoved){
        this.isRemoved=isRemoved;
    }
    User(String id) {
        lock.lock();
        userID = id;
        followees = new LinkedList<>();
        followers = new LinkedList<>();
        topics = "[]";
        questions = "[]";
        lastUpdate = Instant.now();
        lock.unlock();
    }

    boolean isDone() {
        lock.lock();
        if (Duration.between(lastUpdate, Instant.now()).toSeconds() > 600) {
            util.logWarning(String.format("NotComplete %s %d %d %d %d", info, followersICount, followeesICount, topicsICount, questionsICount));
            lock.unlock();
            return true;
        }
        boolean f = info != null && followersICount == -1 && followeesICount == -1 && topicsICount == -1 && questionsICount == -1;
        lock.unlock();
        return f;
    }

//    String getListStr(List<String> l) {
//        String last = l.remove(l.size() - 1);
//        StringBuffer buffer = new StringBuffer();
//        buffer.append("[");
//        for (String s : l) {
//            buffer.append(s);
//            buffer.append(",");
//        }
//        buffer.append(last);
//        buffer.append("]");
//        return buffer.toString();
//    }

    @SuppressWarnings("unchecked")
    void set(String type, String content, int id, int totals) throws JsonProcessingException {
        lastUpdate = Instant.now();
        if (type.equals("info")) {
            info = content;
            return;
        }
        if (type.equals("follower") || type.equals("followee")) {
            List<Map<String, Object>> list = util.loadJsonString(content, List.class);
            LinkedList<String> tmp = new LinkedList<>();
            for (Map<String, Object> m : list) {
                tmp.add((String) m.get("url_token"));
            }
            lock.lock();
            if (type.equals("follower")) {
                followers.addAll(tmp);
                if (followersICount != -1 && ++followersICount >= totals) {
                    followersICount = -1;
                }
            } else {
                followees.addAll(tmp);
                if (followeesICount != -1 && ++followeesICount >= totals) {
                    followeesICount = -1;
                }
            }
            lock.unlock();
            tmp.clear();
            list.clear();
            return;
        }
        if (type.equals("topic")) {
            lock.lock();
            String del = topics.equals("[]") ? "" : ",";
            topics = topics.substring(0, topics.length() - 1) + del + content.substring(1);
            if (topicsICount != -1 && ++topicsICount >= totals) {
                topicsICount = -1;
            }
            lock.unlock();
            return;
        }
        if (type.equals("question")) {
            lock.lock();
            String del = questions.equals("[]") ? "" : ",";
            questions = questions.substring(0, questions.length() - 1) + del + content.substring(1);
            if (questionsICount != -1 && ++questionsICount >= totals) {
                questionsICount = -1;
            }
            lock.unlock();
        }
    }

    LinkedList<String> getFriendsFromElement() {
        util.logInfo("into get friends");
        lock.lock();
        util.logInfo("into lock");
        LinkedList<String> friends = new LinkedList<>();
        friends.addAll(followees);
        friends.addAll(followers);
        util.logInfo("done get friends");
        lock.unlock();
        util.logInfo("out lock");
        return friends;
    }

    public String getString() throws JsonProcessingException {
        String followersString=util.toJsonString(followers).replaceAll(",", ",\n");
        String followeesString=util.toJsonString(followees).replaceAll(",", ",\n");
        return String.format("{\n\"userID\":\"%s\",\n\"info\":%s," +
                        "\n\"followers\":%s,\n\"followees\":%s,\n\"topics\":%s,\n\"questions\":%s\n}",
                userID, info, followersString, followeesString, topics, questions);
    }
}

class ConstructUsers {
    private final ConcurrentMap<String, User> userMap = new ConcurrentHashMap<>(1000);
    private final int workers;
    private final BlockingQueue<DataNode> dataQueue;
    private final BlockingQueue<User> writeQueue;
    private final ConcurrentMap<String, String> errorUsers;
    private User removedUser=new User(true);

    ConstructUsers(int workers,
                   BlockingQueue<DataNode> dataQueue,
                   BlockingQueue<User> writeQueue,
                   ConcurrentMap<String, String> errorUsers) {
        this.workers = workers;
        this.dataQueue = dataQueue;
        this.writeQueue = writeQueue;
        this.errorUsers = errorUsers;
    }

    private synchronized void checkMap() throws InterruptedException {
        int i=0;
        for (String s : userMap.keySet()) {
            User user = userMap.get(s);
            if(user.isRemoved){
                continue;
            }
            i++;
            if (errorUsers.containsKey(s)) {
                userMap.replace(s,removedUser);
            } else if (user.isDone()) {
                {
                    writeQueue.put(user);
                    userMap.replace(s,removedUser);
                }
            }
        }
        util.logInfo("MapSize " + userMap.size()+" valid users "+i);
    }

    private void doTask() {
        try {
            while (true) {
                checkMap();
                DataNode dataNode = dataQueue.poll(10000, TimeUnit.MILLISECONDS);
                if (dataNode == null) {
                    continue;
                }
                String userID = dataNode.user;

                userMap.putIfAbsent(userID, new User(userID));

                User user = userMap.get(userID);
                if (user.isRemoved) {
                    continue;
                }
                try {
                    user.set(dataNode.type, dataNode.data, dataNode.id, dataNode.totalRequestNum);
                    if (user.isDone()) {
                        writeQueue.put(user);
                        userMap.replace(user.userID,removedUser);
                    }
                } catch (JsonProcessingException e) {
                    errorUsers.put(userID, "JSON PROCESSING 1");
                    userMap.replace(user.userID,removedUser);
                    util.logWarning("ERRORUSER 1 " + userID);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    void startThreads() {
        for (int i = 0; i < workers; i++) {
            new Thread(this::doTask).start();
        }
    }
}
