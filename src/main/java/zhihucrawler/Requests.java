package zhihucrawler;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

class RequestNode {
    String user;
    String type;
    String url;
    int id;
    int totalRequestNum;
    int tryCount;

    RequestNode(String user, String type) {
        this(user,type,util.getUrl(user,type),0,-1,0);
    }

    RequestNode(String user, String type, String url, int id, int totalRequestNum, int tryCount) {
        this.user = user;
        this.type = type;
        this.url = url;
        this.id=id;
        this.totalRequestNum=totalRequestNum;
        this.tryCount=tryCount;
    }
}

class ResponseNode {
    RequestNode requestNode;
    String body;
    int status;

    ResponseNode(RequestNode requestNode, int status, String body) {
        this.requestNode=requestNode;
        this.status=status;
        this.body = body;
    }
}

class DataNode{
    String user;
    String type;
    String data;
    int id;
    int totalRequestNum;

    DataNode(String user,String type,int id, int totalRequestNum, String data){
        this.user=user;
        this.type=type;
        this.id=id;
        this.totalRequestNum=totalRequestNum;
        this.data=data;
    }
}

class Requests{
    private final CookieProvider cookieProvider;
    private final ProxyProvider proxyProvider;
    private final int workers;
    private final int tryMax;
    private final BlockingQueue<RequestNode> requestQueue;
    private final BlockingQueue<RequestNode> requestQueue0;
    private final BlockingQueue<ResponseNode> responseQueue;
    private final BlockingQueue<DataNode> dataQueue;
    private final ConcurrentMap<String,String> errorUsers;

    Requests(String cookieFileName, int workers, int tryMax, ConcurrentMap<String,String> errorUsers)throws IOException {
        cookieProvider=new CookieProvider(cookieFileName);
        proxyProvider=new ProxyProvider(Math.min(500,workers*2));
        //proxyProvider=null;
        this.workers=workers;
        this.tryMax=tryMax;
        this.errorUsers=errorUsers;
        requestQueue=new LinkedBlockingQueue<>(workers*2);
        requestQueue0=new LinkedBlockingQueue<>();
        responseQueue=new LinkedBlockingQueue<>(workers*2);
        dataQueue=new LinkedBlockingQueue<>(workers*2);
    }

    BlockingQueue<RequestNode> getRequestQueue() {
        return requestQueue;
    }

    BlockingQueue<RequestNode> getRequestQueue0() {
        return requestQueue0;
    }

    BlockingQueue<ResponseNode> getResponseQueue() {
        return responseQueue;
    }

    BlockingQueue<DataNode> getDataQueue() {
        return dataQueue;
    }

    void startThreads() {
        Thread [] requestThreads=new Thread[workers];
        Thread [] extractorThreads=new Thread[workers];
        for (int i = 0; i < workers; i++) {
            requestThreads[i]=new Thread(new RequestJson(cookieProvider,proxyProvider,tryMax,requestQueue,requestQueue0,responseQueue,errorUsers));
            requestThreads[i].start();
        }
        for (int i = 0; i < workers; i++) {
            extractorThreads[i]=new Thread(new ResponseExtractor(requestQueue0,responseQueue,dataQueue,errorUsers));
            extractorThreads[i].start();
        }
        /*//监测线程，定时打印活着的请求线程数
        new Thread(()->{
            try {
                while (true) {
                    int requestThreadsAliveNum=workers;
                    for(int i=0;i<workers;i++){
                        if(!requestThreads[i].isAlive()){
                            requestThreadsAliveNum--;
                        }
                    }
                    int extractorThreadsAliveNum=workers;
                    for(int i=0;i<workers;i++){
                        if(!extractorThreads[i].isAlive()){
                            extractorThreadsAliveNum--;
                        }
                    }
                    util.logInfo("requestThreadsAliveNum "+requestThreadsAliveNum+" extractorThreadsAliveNum "+extractorThreadsAliveNum);
                    Thread.sleep(10000);
                }
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }).start();*/
    }
}
