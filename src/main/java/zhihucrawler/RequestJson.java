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
    private final ConcurrentMap<String,String> errorUsers;
    private final Request request;

    RequestJson(CookieProvider cookieProvider,
                ProxyProvider proxyProvider,
                int tryMax,
                BlockingQueue<RequestNode> requestQueue,
                BlockingQueue<RequestNode> requestQueue0,
                BlockingQueue<ResponseNode> responseQueue,
                ConcurrentMap<String,String> errorUsers,
                boolean verbose) {
        this.request=new Request(cookieProvider,proxyProvider,tryMax,verbose);
        this.requestQueue = requestQueue;
        this.requestQueue0 = requestQueue0;
        this.responseQueue = responseQueue;
        this.errorUsers = errorUsers;
    }


    @Override
    public void run() {
        String url=null;
        RequestNode requestNode=null;
        try {
            //noinspection InfiniteLoopStatement
            while (true) {
                if ((requestNode = requestQueue0.poll(1000, TimeUnit.MILLISECONDS)) == null) {
                    requestNode = requestQueue.poll(1000, TimeUnit.MILLISECONDS);
                    if(requestNode==null)continue;
                }
                String user = requestNode.user;
                if (errorUsers.containsKey(user)) {
                    continue;
                }
                url= requestNode.url;
                try {
                    if(user.equals("tiancaomei")&&requestNode.type.equals("follower")){
                        util.logInfo("RRRR5 "+url);
                    }
                    HttpResponse<String> response = request.request(url);
                    int status = response.statusCode();
                    String body = response.body();
                    responseQueue.put(new ResponseNode(requestNode, status, body));
                }catch (IOException e){
                    requestQueue0.put(requestNode);
                    if(e.getMessage().equals("Network Error")) {
                        util.logSevere("HTTPNORESPONSE ");
                        throw new InterruptedException();
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
