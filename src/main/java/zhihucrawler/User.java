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
    //public StringBuilder folowersCompleteInfo;
    private int followersICount = 0;
    public List<String> followees;
    //public StringBuilder foloweesCompleteInfo;
    private int followeesICount = 0;
    public StringBuilder topics;
    private int topicsICount = 0;
    public StringBuilder questions;
    private int questionsICount = 0;
    private Instant lastUpdate;
    private ReentrantLock lock = new ReentrantLock();

    boolean isDone = false;

    //private static Pattern pattern = Pattern.compile("\"url_token\"\\s*:\\s*\"([^\"]+)\"");
    User(boolean isDone) {
        this.isDone = isDone;
    }

    User(String id) {
        lock.lock();
        userID = id;
        followees = new LinkedList<>();
        followers = new LinkedList<>();
        lastUpdate = Instant.now();
        lock.unlock();
    }

    /*用户的请求是否都已完成*/
    boolean isDone() {
        try {
            lock.lock();
            if(isDone){  //使isDone函数只返回一次真，从而只写入一次
                return false;
            }
        /*if (Duration.between(lastUpdate, Instant.now()).toSeconds() > 600) {
            util.logWarning(String.format("NotComplete %s %d %d %d %d", info, followersICount, followeesICount, topicsICount, questionsICount));
            lock.unlock();
            return true;
        }*/
            isDone = (info != null && followersICount == -1 && followeesICount == -1 && topicsICount == -1 && questionsICount == -1);
            return isDone;
        }finally {
            lock.unlock();
        }
    }

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
                /*if(folowersCompleteInfo==null){
                    folowersCompleteInfo=new StringBuilder(totals*20*200);
                }
                String del = folowersCompleteInfo.length()==0|content.equals("[]")|content.equals("[ ]") ? "" : ",";
                folowersCompleteInfo.append(del).append(content, 1, content.length() - 1);*/
                if (followersICount != -1 && ++followersICount >= totals) {
                    followersICount = -1;
                }
            } else {
                followees.addAll(tmp);
                /*if(foloweesCompleteInfo==null){
                    foloweesCompleteInfo=new StringBuilder(totals*20*200);
                }
                String del = foloweesCompleteInfo.length()==0|content.equals("[]")|content.equals("[ ]") ? "" : ",";
                foloweesCompleteInfo.append(del).append(content, 1, content.length() - 1);*/
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
            String del = topics.length() == 0|content.equals("[]")|content.equals("[ ]") ? "" : ",";
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
            String del = questions.length() == 0|content.equals("[]")|content.equals("[ ]") ? "" : ",";
            questions.append(del).append(content, 1, content.length() - 1);
            if (questionsICount != -1 && ++questionsICount >= totals) {
                questionsICount = -1;
            }
            lock.unlock();
        }
    }

    List<String> getFriends() {
        lock.lock();
        ArrayList<String> friends = new ArrayList<>(followees.size() + followers.size() + 1000);
        friends.addAll(followees);
        friends.addAll(followers);
        lock.unlock();
        return friends;
    }

    /*获取用户数据的json表示*/
    public String getJsonString() throws JsonProcessingException {
        lock.lock();
        try {
            String followersString = util.toJsonString(followers).replaceAll(",", ",\n");
            String followeesString = util.toJsonString(followees).replaceAll(",", ",\n");
            /*String res = String.format("{\n\"userID\":\"%s\",\n\"info\":%s,\n" +
                            "\"followers\":%s,\n\"followees\":%s,\n\"topics\":[%s],\n\"questions\":[%s],\n" +
                            "\"followersCompleteInfo\":[%s],\n\"followeesCompleteInfo\":[%s]}",
                    userID, info, followersString, followeesString, topics, questions, folowersCompleteInfo, foloweesCompleteInfo);*/
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


    String getString() {
        lock.lock();
        String res = String.format("(%s %s %d %d %d %d)",
                userID, info == null, followersICount, followeesICount, topicsICount, questionsICount);
        lock.unlock();
        return res;
    }
/*
    String getString() {
        lock.lock();
        String res = String.format("%s%s%s%s%s%s",
                userID, info, topics, questions, folowersCompleteInfo, foloweesCompleteInfo);
        lock.unlock();
        return res;
    }*/
}
