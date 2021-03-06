package zhihucrawler;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;


class ConstructUsers {
    private final ConcurrentMap<String, User> userMap = new ConcurrentHashMap<>(1000);
    private final int workers;
    private final BlockingQueue<DataNode> dataQueue;
    private final BlockingQueue<User> writeQueue;
    private final ConcurrentMap<String, String> errorUsers;
    private User removedUser = new User(true);

    ConstructUsers(int workers,
                   BlockingQueue<DataNode> dataQueue,
                   BlockingQueue<User> writeQueue,
                   ConcurrentMap<String, String> errorUsers) {
        this.workers = workers;
        this.dataQueue = dataQueue;
        this.writeQueue = writeQueue;
        this.errorUsers = errorUsers;
    }

    private void checkMap(int flag) {
        int i = 0;
        //long x=0;
        StringBuilder users = new StringBuilder();
        for (String s : userMap.keySet()) {
            User user = userMap.get(s);
            if (user.isDone) {
                continue;
            }
            i++;
            users.append("\n In Map").append(user.getString());
            //if(flag==1)x+=user.getString().length();
            if (errorUsers.containsKey(s)) {
                userMap.replace(s, removedUser);
            }
        }
        util.logInfo("MapSize " + userMap.size() + " valid users " + i + users);
        //if(flag==1)util.logInfo(String.format("MapData %.3f MB ",x/1000000.0));
    }

    private void doTask() {
        Instant last = Instant.now();
        try {
            while (true) {
                synchronized (this) {
                    if (Duration.between(last, Instant.now()).toSeconds() > 10) {
                        checkMap(0);
                        last = Instant.now();
                    }
                }
                DataNode dataNode = dataQueue.poll(10000, TimeUnit.MILLISECONDS);
                if (dataNode == null) {
                    continue;
                }
                String userID = dataNode.user;

                userMap.putIfAbsent(userID, new User(userID));

                User user = userMap.get(userID);
                if (user.isDone) {
                    continue;
                }
                try {
                    user.set(dataNode.type, dataNode.data, dataNode.id, dataNode.totalRequestNum);
                    if (user.isDone()) {
                        writeQueue.put(user);
                        userMap.replace(user.userID, removedUser);
                    }
                } catch (JsonProcessingException e) {
                    errorUsers.put(userID, "JSON PROCESSING 1");
                    userMap.replace(user.userID, removedUser);
                    util.logWarning("ERRORUSER 1 " + userID);
                }
            }
        } catch (InterruptedException e) {
            util.logWarning("ConstructUsers Interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    void startThreads() {
        for (int i = 0; i < workers; i++) {
            new Thread(this::doTask).start();
        }
    }
}
