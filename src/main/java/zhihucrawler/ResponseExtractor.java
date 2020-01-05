package zhihucrawler;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;

class Paging {
    public boolean is_end;
    public boolean is_start;
    public String next;
    public String previous;
    public int totals;
}

class ResponseContent {
    public Paging paging;
    public List<Map<String,Object>> data;
}
public class ResponseExtractor implements Runnable{
    private final BlockingQueue<RequestNode> requestQueue0;
    private final BlockingQueue<ResponseNode> responseQueue;
    private final BlockingQueue<DataNode> dataQueue;
    private final ConcurrentMap<String,String> errorUsers;
    private final int maxEmptyTry=3;

    ResponseExtractor(BlockingQueue<RequestNode> requestQueue0,
                      BlockingQueue<ResponseNode> responseQueue,
                      BlockingQueue<DataNode> dataQueue,
                      ConcurrentMap<String,String> errorUsers){
        this.requestQueue0 = requestQueue0;
        this.responseQueue = responseQueue;
        this.dataQueue = dataQueue;
        this.errorUsers = errorUsers;
    }
    private ResponseContent extractResponse(String content)throws JsonProcessingException {
        return util.loadJsonString(content, ResponseContent.class);
    }


    @Override
    public void run() {
        try {
            //noinspection InfiniteLoopStatement
            while(true){
                ResponseNode responseNode=responseQueue.take();
                int status=responseNode.status;
                String body=responseNode.body;
                String user=responseNode.requestNode.user;
                String url=responseNode.requestNode.url;
                int tryCount=responseNode.requestNode.tryCount;
                String type=responseNode.requestNode.type;
                int id=responseNode.requestNode.id;
                int oldTotalRequestNum=responseNode.requestNode.totalRequestNum;

                if(user.equals("tiancaomei")&&type.equals("follower")){
                    util.logInfo("RRRR2 "+oldTotalRequestNum);
                    util.logInfo("RRRR1 "+url);
                }

                if (status != 200) {
                    if(tryCount>maxEmptyTry){
                        errorUsers.put(user,status+" "+body);
                        util.logWarning("ERRORUSER 3 "+user+" "+status+" "+body);
                    }
                    else {
                        requestQueue0.put(new RequestNode(user, type, url, id, oldTotalRequestNum,tryCount + 1));
                    }
                    continue;
                }

                if(type.equals("info")){
                    dataQueue.put(new DataNode(user,type,id,1,body));
                }
                else{
                    try{
                        ResponseContent content=extractResponse(body);  //TODO change to regex
                        //int totals=content.paging.totals;
                        boolean isEnd=content.paging.is_end;
                        List<Map<String,Object>> data =content.data;

                        //if(totals-(20*id)>0&&data.size()==0){

                        if(user.equals("tiancaomei")&&type.equals("follower")){
                            util.logInfo("RRRR4 "+url);
                        }
                        if(data.size()==0){
                            if(tryCount<maxEmptyTry){
                                requestQueue0.put(new RequestNode(user,type,url,id,oldTotalRequestNum,tryCount+1));
                                continue;
                            }
                            /*errorUsers.put(user,String.format("Empty data %s %d %d %s",responseNode.requestNode.type,id,totals,url));
                                util.logWarning("ERRORUSER 5 Empty data "+user);*/
                        }
                        if(user.equals("tiancaomei")&&type.equals("follower")){
                            util.logInfo("RRRR3 "+url);
                        }
                        if(id==0){
                            int totals=content.paging.totals;
                            if(totals==0){
                                if(tryCount<maxEmptyTry){
                                    requestQueue0.put(new RequestNode(user,type,url,id,oldTotalRequestNum,tryCount+1));
                                }else {
                                    /*errorUsers.put(user, String.format("Empty data %s %d %d %s", responseNode.requestNode.type, id, totals, url));
                                    util.logWarning("ERRORUSER 5 Empty data " + user);*/
                                    dataQueue.put(new DataNode(user,type,id,1,util.toJsonString(data)));
                                }
                                continue;
                            }
                            int totalRequestNum=(totals-1)/20+1;
                            dataQueue.put(new DataNode(user,type,id,totalRequestNum,util.toJsonString(data)));
                            for(int i=1;i<totalRequestNum;i++){
                                String newUrl=url.replace("offset=0","offset="+(i*20));
                                requestQueue0.put(new RequestNode(user,type,newUrl,i,totalRequestNum,0));
                            }
                        }else{
                            dataQueue.put(new DataNode(user,type,id,oldTotalRequestNum,util.toJsonString(data)));
                            /*if(!isEnd&&oldTotalRequestNum-1==id){
                                requestQueue0.put(new RequestNode(user,type,content.paging.next,id+1,id+2,0));
                            }*/
                        }
                        content.data.clear();
                    }catch (JsonProcessingException e){
                        if(tryCount>maxEmptyTry){
                            errorUsers.put(user,"JSON PROCESSING 2");
                            util.logWarning("ERRORUSER 2 "+user);
                        }
                        else {
                            requestQueue0.put(new RequestNode(user, type, url, id, oldTotalRequestNum,tryCount + 1));
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
