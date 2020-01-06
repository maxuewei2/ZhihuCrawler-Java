package zhihucrawler;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

public class RequestJson implements Runnable {
    private final BlockingQueue<RequestNode> requestQueue;
    private final BlockingQueue<RequestNode> requestQueue0;
    private final BlockingQueue<ResponseNode> responseQueue;
    private final ConcurrentMap<String, String> errorUsers;
    private final Request request;

    RequestJson(CookieProvider cookieProvider,
                ProxyProvider proxyProvider,
                int tryMax,
                BlockingQueue<RequestNode> requestQueue,
                BlockingQueue<RequestNode> requestQueue0,
                BlockingQueue<ResponseNode> responseQueue,
                ConcurrentMap<String, String> errorUsers,
                boolean verbose) {
        this.request = new Request(cookieProvider, proxyProvider, tryMax, verbose);
        this.requestQueue = requestQueue;
        this.requestQueue0 = requestQueue0;
        this.responseQueue = responseQueue;
        this.errorUsers = errorUsers;
    }


    @Override
    public void run() {
        RequestNode requestNode = null;
        int httpErrorCount=0;
        try {
            //noinspection InfiniteLoopStatement
            while (true) {
                boolean newRequestFlag=false;
                if (requestNode == null) {
                    if ((requestNode = requestQueue0.poll(1000, TimeUnit.MILLISECONDS)) == null) {
                        requestNode = requestQueue.poll(1000, TimeUnit.MILLISECONDS);
                        if (requestNode == null) continue;
                        synchronized (requestQueue){
                            requestQueue.notify();
                        }
                    }
                    newRequestFlag=true;
                }
                if (errorUsers.containsKey(requestNode.user)) {
                    requestNode=null;
                    continue;
                }

                try {
                    HttpResponse<String> response;
                    if (newRequestFlag) {
                        response = request.request(requestNode.url);
                    } else {
                        response = request.request(requestNode.url, null);
                        httpErrorCount=0;
                    }
                    responseQueue.put(new ResponseNode(requestNode, response.statusCode(), response.body()));
                    requestNode=null;
                } catch (IOException e) {
                    if (e.getMessage().equals("Network Error")) {
                        Thread.sleep(500);
                        if(httpErrorCount++>10) {
                            util.logSevere("nonproxy HTTPNORESPONSE 10 times\nThread terminate.",e);
                            errorUsers.put(requestNode.user, "Network Error");
                            Thread.currentThread().interrupt();
                        }
                        util.logWarning("nonproxy HTTPNORESPONSE ",e);
                    }else {
                        util.logWarning("proxy HTTPNORESPONSE ", e);
                    }
                    //requestQueue0.put(requestNode);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
