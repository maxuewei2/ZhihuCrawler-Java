package zhihucrawler;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

public class RequestJson implements Runnable {
    volatile int status;
    private static final int maxStatusTry=3;
    private final BlockingQueue<RequestNode> requestQueue;
    private final BlockingQueue<RequestNode> requestQueue0;
    private final BlockingQueue<ResponseNode> responseQueue;
    private final ConcurrentMap<String, String> errorUsers;
    private final Request request;
    private final int sleepMills;
    private final static String[] refererKey = {"/followers", "/following", "/following/topics", "/following/questions"};

    RequestJson(CookieProvider cookieProvider,
                ProxyProvider proxyProvider,
                int maxTryNum,
                int sleepMills,
                BlockingQueue<RequestNode> requestQueue,
                BlockingQueue<RequestNode> requestQueue0,
                BlockingQueue<ResponseNode> responseQueue,
                ConcurrentMap<String, String> errorUsers) {
        this.request = new Request(cookieProvider, proxyProvider, maxTryNum);
        this.requestQueue = requestQueue;
        this.requestQueue0 = requestQueue0;
        this.responseQueue = responseQueue;
        this.errorUsers = errorUsers;
        this.sleepMills=sleepMills;
    }

    int checkStatus(HttpResponse<String> response, RequestNode requestNode)throws InterruptedException{
        // status不为200时重复请求，多次失败则记录到errorUsers
        int status=response.statusCode();
        String body = response.body();
        String user = requestNode.user;
        String type = requestNode.type;
        String url = requestNode.url;
        int id = requestNode.id;
        int oldTotalRequestNum = requestNode.totalRequestNum;
        int tryCount = requestNode.tryCount;

        if (status != 200 && status!=403) {
            if (tryCount > maxStatusTry) {
                errorUsers.put(user, status + " " + body);
                util.logWarning("ERRORUSER 3 " + user + " " + status + " " + body);
                return -1;
            } else {
                requestQueue0.put(new RequestNode(user, type, url, id, oldTotalRequestNum, tryCount + 1));
                return 0;
            }
        }
        return status;
    }

    String getReferer(String user,String type){
        String prefix = "https://www.zhihu.com/people/" + user;
        switch (type) {
            case "follower":
                return prefix + refererKey[0];
            case "followee":
                return prefix + refererKey[1];
            case "topic":
                return prefix + refererKey[2];
            case "question":
                return prefix + refererKey[3];
            default:
                return "";
        }
    }



    @Override
    public void run() {
        RequestNode requestNode = null;
        int httpErrorCount=0;
        try {
            //noinspection InfiniteLoopStatement
            while (true) {
                Thread.sleep(System.currentTimeMillis()%sleepMills);
                boolean newRequestFlag=false;
                if (requestNode == null) {
                    //先在requestQueue0取，取不到再到requestQueue取
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
                    String referer=getReferer(requestNode.user,requestNode.type);
                    response = request.request(requestNode.url,referer);
                    /*if (newRequestFlag) {
                        response = request.request(requestNode.url);
                    } else {
                        response = request.request(requestNode.url, null);
                        httpErrorCount=0;
                    }*/
                    int flag=checkStatus(response,requestNode);
                    if(flag==0||flag==-1){
                        requestNode=null;
                        continue;
                    }
                    if(flag==403){
                        status=-1;
                        /*util.print(response.body());
                        response = request.request("https://www.zhihu.com/api/v4/anticrawl/captcha_appeal",referer);
                        util.print(response.body());
                        System.exit(0);*/
                        if(request.getProxy()==null){
                            util.logWarning("403 null");
                            while(!request.changeProxy()){
                                Thread.sleep(500);
                            }
                        }
                        //util.logSevere("Response status code 403. Need login.");
                        //return;
                        else{
                            util.logWarning("403 "+request.getProxy());
                            request.getProxy().setError();
                            request.changeProxy();
                        }
                        continue;
                    }
                    status=0;
                    responseQueue.put(new ResponseNode(requestNode, response.statusCode(), response.body()));
                    requestNode=null;
                } catch (IOException e) {
                    if (e.getMessage().equals("Network Error")) {
                        Thread.sleep(500);
                        /*if(httpErrorCount++>10) {
                            util.logSevere("nonproxy HTTPNORESPONSE 10 times\nThread terminate.",e);
                            errorUsers.put(requestNode.user, "Network Error");
                            Thread.currentThread().interrupt();
                        }*/
                        util.logWarning("nonproxy HTTPNORESPONSE ",e);
                    }else {
                        util.logWarning("proxy HTTPNORESPONSE ", e);
                    }
                    request.changeProxy();
                }
            }
        } catch (InterruptedException e) {
            util.logWarning("RequestJson Interrupted",e);
            Thread.currentThread().interrupt();
        }
    }
}
