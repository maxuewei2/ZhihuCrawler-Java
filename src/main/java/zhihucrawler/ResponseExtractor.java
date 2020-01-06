package zhihucrawler;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class Paging {
    public boolean is_end;
    public boolean is_start;
    public String next;
    public String previous;
    public int totals;
}

class ResponseContent {
    public Paging paging;
    public List<Map<String, Object>> data;
}

public class ResponseExtractor implements Runnable {
    private static final AtomicInteger receivedBytesNum = new AtomicInteger(0);
    private static final AtomicInteger receivedMBNum = new AtomicInteger(0);
    private final BlockingQueue<RequestNode> requestQueue0;
    private final BlockingQueue<ResponseNode> responseQueue;
    private final BlockingQueue<DataNode> dataQueue;
    private final ConcurrentMap<String, String> errorUsers;
    private final int maxEmptyTry = 3;

    ResponseExtractor(BlockingQueue<RequestNode> requestQueue0,
                      BlockingQueue<ResponseNode> responseQueue,
                      BlockingQueue<DataNode> dataQueue,
                      ConcurrentMap<String, String> errorUsers) {
        this.requestQueue0 = requestQueue0;
        this.responseQueue = responseQueue;
        this.dataQueue = dataQueue;
        this.errorUsers = errorUsers;
    }

    private void putJsonRequestUrls(String user, Map<String, Object> info) throws InterruptedException {
        String[] types = {"follower", "followee", "topic", "question"};
        String[] infoKey = {"follower_count", "following_count", "following_topic_count", "following_question_count"};
        if (user == null) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            String type = types[i];
            String url = util.getUrl(user, type);
            int totals = (int) info.get(infoKey[i]);
            if (totals == 0) {
                dataQueue.put(new DataNode(user, type, 0, 0, "[]"));
                continue;
            }
            int totalRequestNum = (totals - 1) / 20 + 1;
            for (int j = 0; j < totalRequestNum; j++) {
                String newUrl = url.replace("offset=0", "offset=" + (j * 20));
                requestQueue0.put(new RequestNode(user, type, newUrl, j, totalRequestNum, 0));
            }
        }
    }

    @Override
    public void run() {
        Instant last = Instant.now();
        try {
            //noinspection InfiniteLoopStatement
            while (true) {
                ResponseNode responseNode = responseQueue.take();
                int status = responseNode.status;
                String body = responseNode.body;
                RequestNode requestNode = responseNode.requestNode;
                String user = requestNode.user;
                String type = requestNode.type;
                String url = requestNode.url;
                int id = requestNode.id;
                int oldTotalRequestNum = requestNode.totalRequestNum;
                int tryCount = requestNode.tryCount;

                if (receivedBytesNum.addAndGet(body.length()) > 10000000) {
                    synchronized (receivedBytesNum) {
                        if (receivedBytesNum.get() > 10000000) {
                            while (true) {
                                int tmp = receivedBytesNum.get();
                                if (receivedBytesNum.compareAndSet(tmp, tmp % 10000000)) break;
                            }
                            receivedMBNum.addAndGet(10);
                            util.print(receivedMBNum + " MB data received");
                            Instant now = Instant.now();
                            util.print(String.format("receiving speed %.2f KB/s", 10.0 * 1000 / (Duration.between(last, now).toMillis() / 1000.0)));
                            last = now;
                        }
                    }
                }

                if (status != 200) {
                    if (tryCount > maxEmptyTry) {
                        errorUsers.put(user, status + " " + body);
                        util.logWarning("ERRORUSER 3 " + user + " " + status + " " + body);
                    } else {
                        requestQueue0.put(new RequestNode(user, type, url, id, oldTotalRequestNum, tryCount + 1));
                    }
                    continue;
                }
                try {
                    if (type.equals("info")) {
                        dataQueue.put(new DataNode(user, type, id, 1, body));
                        Map<String, Object> info = util.loadJsonString(body, Map.class);
                        putJsonRequestUrls(user, info);
                        continue;
                    }
                    ResponseContent content = util.loadJsonString(body, ResponseContent.class);  //TODO change to regex
                    List<Map<String, Object>> data = content.data;

                    if (data.size() == 0) {
                        if (tryCount < maxEmptyTry) {
                            requestQueue0.put(new RequestNode(user, type, url, id, oldTotalRequestNum, tryCount + 1));

                        } else {
                            errorUsers.put(user, String.format("Empty data %s %d %d %s", type, id, oldTotalRequestNum, url));
                            util.logWarning("ERRORUSER 5 Empty data " + user);
                        }
                        continue;
                    }
                    dataQueue.put(new DataNode(user, type, id, oldTotalRequestNum, util.toJsonString(data)));
                    content.data.clear();
                } catch (JsonProcessingException e) {
                    if (tryCount > maxEmptyTry) {
                        errorUsers.put(user, "JSON PROCESSING 2");
                        util.logWarning("ERRORUSER 2 " + user);
                    } else {
                        requestQueue0.put(new RequestNode(user, type, url, id, oldTotalRequestNum, tryCount + 1));
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
