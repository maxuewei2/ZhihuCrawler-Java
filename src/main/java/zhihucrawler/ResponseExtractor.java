package zhihucrawler;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/*对应知乎返回的json数据的paging部分*/
class Paging {
    public boolean is_end;
    public boolean is_start;
    public String next;
    public String previous;
    public int totals;
}

/*对应知乎返回json数据的格式*/
class ResponseContent {
    public Paging paging;
    public List<Map<String, Object>> data;
}

public class ResponseExtractor implements Runnable {
    private static final AtomicInteger receivedBytesNum = new AtomicInteger(0);
    private static final AtomicInteger receivedMBNum = new AtomicInteger(0);
    private static Instant last = Instant.now();
    private final static int tenMB=10*1024*1024;
    private final static int maxEmptyTry = 3;

    private final BlockingQueue<RequestNode> requestQueue0;
    private final BlockingQueue<ResponseNode> responseQueue;
    private final BlockingQueue<DataNode> dataQueue;
    private final ConcurrentMap<String, String> errorUsers;


    ResponseExtractor(BlockingQueue<RequestNode> requestQueue0,
                      BlockingQueue<ResponseNode> responseQueue,
                      BlockingQueue<DataNode> dataQueue,
                      ConcurrentMap<String, String> errorUsers) {
        this.requestQueue0 = requestQueue0;
        this.responseQueue = responseQueue;
        this.dataQueue = dataQueue;
        this.errorUsers = errorUsers;
    }

    /*从info数据中提取其他数据的总数，并提交请求*/
    private void putJsonRequestUrls(String user, Map<String, Object> info) throws InterruptedException {
        if (user == null) {
            return;
        }
        String[] types = {"follower", "followee", "topic", "question"};
        String[] infoKey = {"follower_count", "following_count", "following_topic_count", "following_question_count"};
        for (int i = 0; i < types.length; i++) {
            String type = types[i];
            int totals = (int) info.get(infoKey[i]);
            if (totals == 0) {
                dataQueue.put(new DataNode(user, type, 0, 0, "[]"));
                continue;
            }
            int totalRequestNum = (totals - 1) / 20 + 1;
            String url = util.getUrl(user, type);
            for (int j = 0; j < totalRequestNum; j++) {
                String newUrl = url.replace("offset=0", "offset=" + (j * 20));
                requestQueue0.put(new RequestNode(user, type, newUrl, j, totalRequestNum, 0));
            }
        }
    }

    @Override
    public void run() {

        try {
            //noinspection InfiniteLoopStatement
            while (true) {
                ResponseNode responseNode = responseQueue.take();
                String body = responseNode.body;
                RequestNode requestNode = responseNode.requestNode;
                String user = requestNode.user;
                String type = requestNode.type;
                String url = requestNode.url;
                int id = requestNode.id;
                int oldTotalRequestNum = requestNode.totalRequestNum;
                int tryCount = requestNode.tryCount;

                /*累计收到的数据大小，每获取10MB，计算请求速度，并打印*/

                if (receivedBytesNum.addAndGet(body.length()) > tenMB) {
                    synchronized (receivedBytesNum) {
                        if (receivedBytesNum.get() > tenMB) {
                            while (true) {
                                int tmp = receivedBytesNum.get();
                                if (receivedBytesNum.compareAndSet(tmp, tmp % tenMB)) break;
                            }
                            Instant now = Instant.now();
                            double secs=(Duration.between(last, now).toMillis() / 1000.0);
                            util.print(""+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                            util.print(String.format("%d MB data received.\nThe last 10MB take %.2f seconds.",receivedMBNum.addAndGet(10),secs));
                            util.print(String.format("Receiving speed: %.2f KB/s\n", 10240.0 / secs));
                            last = now;
                        }
                    }
                }
                try {
                    if (type.equals("info")) {
                        dataQueue.put(new DataNode(user, type, id, 1, body));
                        Map<String, Object> info =  util.loadJsonString(body, Map.class);
                        //根据info数据构造其他数据请求
                        putJsonRequestUrls(user, info);
                        continue;
                    }
                    ResponseContent content = util.loadJsonString(body, ResponseContent.class);
                    List<Map<String, Object>> data = content.data;

                    //请求到的数据为空时重复请求，多次失败则说明知乎数据有问题，忽略该错误并继续
                    if (data.size() == 0) {
                        if (tryCount < maxEmptyTry) {
                            requestQueue0.put(new RequestNode(user, type, url, id, oldTotalRequestNum, tryCount + 1));
                            continue;
                        } /*else {
                            errorUsers.put(user, String.format("Empty data %s %d %d %s", type, id, oldTotalRequestNum, url));
                            util.logWarning("ERRORUSER 5 Empty data " + user);
                        }*/

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
            util.logWarning("ResponseExtractor Interrupted",e);
            Thread.currentThread().interrupt();
        }
    }
}
