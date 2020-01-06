package zhihucrawler;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

class User {
    public String userID;
    public String info;
    public List<String> followers;
    //    public StringBuilder folowersCompleteInfo;
    private int followersICount = 0;
    public List<String> followees;
    //    public StringBuilder foloweesCompleteInfo;
    private int followeesICount = 0;
    public StringBuilder topics;
    private int topicsICount = 0;
    public StringBuilder questions;
    private int questionsICount = 0;
    private Instant lastUpdate;
    private ReentrantLock lock = new ReentrantLock();
    boolean isRemoved = false;

    //private static Pattern pattern = Pattern.compile("\"url_token\"\\s*:\\s*\"([^\"]+)\"");
    User(boolean isRemoved) {
        this.isRemoved = isRemoved;
    }

    User(String id) {
        lock.lock();
        userID = id;
        followees = new LinkedList<>();
        followers = new LinkedList<>();
        lastUpdate = Instant.now();
        lock.unlock();
    }

    boolean isDone() {
        lock.lock();
        /*if (Duration.between(lastUpdate, Instant.now()).toSeconds() > 600) {
            util.logWarning(String.format("NotComplete %s %d %d %d %d", info, followersICount, followeesICount, topicsICount, questionsICount));
            lock.unlock();
            return true;
        }*/
        boolean f = (info != null && followersICount == -1 && followeesICount == -1 && topicsICount == -1 && questionsICount == -1);
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
//                if(folowersCompleteInfo==null){
//                    folowersCompleteInfo=new StringBuilder(totals*20*200);
//                }
//                String del = folowersCompleteInfo.length()==0 ? "" : ",";
//                folowersCompleteInfo.append(del).append(content, 1, content.length() - 1);
                if (followersICount != -1 && ++followersICount >= totals) {
                    followersICount = -1;
                }
            } else {
                followees.addAll(tmp);
//                if(foloweesCompleteInfo==null){
//                    foloweesCompleteInfo=new StringBuilder(totals*20*200);
//                }
//                String del = foloweesCompleteInfo.length()==0 ? "" : ",";
//                foloweesCompleteInfo.append(del).append(content, 1, content.length() - 1);
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
            if (topics == null) {
                topics = new StringBuilder(totals * 20 * 200);
            }
            String del = topics.length() == 0 ? "" : ",";
            topics.append(del).append(content, 1, content.length() - 1);
            if (topicsICount != -1 && ++topicsICount >= totals) {
                topicsICount = -1;
            }
            lock.unlock();
            return;
        }
        if (type.equals("question")) {
            lock.lock();
            if (questions == null) {
                questions = new StringBuilder(totals * 20 * 200);
            }
            String del = questions.length() == 0 ? "" : ",";
            questions.append(del).append(content, 1, content.length() - 1);
            if (questionsICount != -1 && ++questionsICount >= totals) {
                questionsICount = -1;
            }
            lock.unlock();
        }
    }

    List<String> getFriendsFromElement() {
        lock.lock();
        ArrayList<String> friends = new ArrayList<>(followees.size() + followers.size() + 1000);
        friends.addAll(followees);
        friends.addAll(followers);
        lock.unlock();
        return friends;
    }

    public String getString() throws JsonProcessingException {
        lock.lock();
        try {
            String followersString = util.toJsonString(followers).replaceAll(",", ",\n");
            String followeesString = util.toJsonString(followees).replaceAll(",", ",\n");
//            String res = String.format("{\n\"userID\":\"%s\",\n\"info\":%s,\n" +
//                            "\"followers\":%s,\n\"followees\":%s,\n\"topics\":[%s],\n\"questions\":[%s],\n" +
//                            "\"followersCompleteInfo\":[%s],\n\"followeesCompleteInfo\":[%s]}",
//                    userID, info, followersString, followeesString, topics, questions, folowersCompleteInfo, foloweesCompleteInfo);
            String res = String.format("{\n\"userID\":\"%s\",\n\"info\":%s,\n" +
                            "\"followers\":%s,\n\"followees\":%s,\n\"topics\":[%s],\n\"questions\":[%s]\n}",
                    userID, info, followersString, followeesString, topics, questions);
            lock.unlock();
            return res;
        } catch (JsonProcessingException e) {
            lock.unlock();
            throw e;
        }
    }

    String getString1() {
        lock.lock();
//        String res = String.format("%s%s%s%s%s%s",
//                userID, info, topics, questions, folowersCompleteInfo, foloweesCompleteInfo);
        String res = String.format("%s%s%s%s",
                userID, info, topics, questions);
        lock.unlock();
        return res;
    }
}