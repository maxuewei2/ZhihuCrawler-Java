package zhihucrawler;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Proxy{
    private String proxyString;
    volatile boolean errorFlag=false;

    Proxy(String proxyString){
        this.proxyString=proxyString;
    }

    boolean isValid(){
        return !errorFlag;
    }

    public void setError() {
        this.errorFlag=true;
    }

    String getIP() {
        return proxyString.split(":")[0];
    }

    int getPort() {
        return Integer.parseInt(proxyString.split(":")[1]);
    }

    public String toString(){
        return proxyString;
    }
}

class ProxyProvider {
    private final BlockingDeque<Proxy> proxies;
    private final int minNum;
    private final int maxNum;
    //private final HashSet<String> crawledProxies;
    private final Pattern pattern=Pattern.compile("alt=\"Cn\" /></td>\\s+<td>([^<]+)</td>\\s+<td>([^<]+)</td>");

    ProxyProvider(int minNum){
        this.minNum=minNum;
        this.maxNum=3*minNum;
        //crawledProxies=new HashSet<>();
        proxies=new LinkedBlockingDeque<>();
        new Thread(this::crawlProxies).start();
    }

    private ArrayList<String> crawlPage(Request request, int pageNum) throws IOException, InterruptedException {
        String url="https://www.xicidaili.com/wn/"+pageNum;
        HttpResponse<String> response=request.request(url);

        ArrayList<String> proxyList=new ArrayList<>();
        if(response.statusCode()==200){
            Matcher matcher=pattern.matcher(response.body());
            while(matcher.find()){
                String ip=matcher.group(1);
                String port=matcher.group(2);
                proxyList.add(ip+":"+port);
            }
        }else{
            //request.changeProxy();
        }
        return proxyList;
    }

    boolean test(String proxyString){
        Request testRequest=new Request(null,null,1,true);
        testRequest.setProxy(new Proxy(proxyString));
        try {
            testRequest.request("https://www.zhihu.com");
        } catch (InterruptedException e) {
            assert true;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    ArrayList<String> check(ArrayList<String> proxyStrings)throws InterruptedException{
        //proxyStrings.removeAll(crawledProxies);
        int numProxies=proxyStrings.size();
        boolean []res=new boolean[numProxies];
        Thread [] threads=new Thread[numProxies];
        for(int i=0;i<numProxies;i++){
            int finalI = i;
            Thread thread=new Thread(()->{
                res[finalI]=test(proxyStrings.get(finalI));
            });
            threads[i]=thread;
            thread.start();
        }
        for(int i=0;i<numProxies;i++) {
            threads[i].join();
        }
        for(int i=numProxies-1;i>=0;i--){
            if(!res[i]){
                proxyStrings.remove(i);
            }
        }
        //util.print(proxyStrings+" "+proxyStrings.size()+" "+proxies.size());
        return proxyStrings;
    }

    @SuppressWarnings("unchecked")
    private void crawlProxies(){
        Request request=new Request(null,null,3,true);
        request.setProxy(new Proxy("127.0.0.1:12333"));
        try {
            /*String content=util.loadFile("proxies.txt");
            ArrayList<String> tmp=util.loadJsonString(content,ArrayList.class);
            for(String s:tmp){
                Proxy proxy=new Proxy(s);
                if(test(testRequest,proxy)) {
                    proxies.offer(new Proxy(s));
                }
            }
            request.changeProxy();*/
            while(true){
                boolean fullFlag=false;
                int pageNum=1;
                while(!fullFlag){
                    ArrayList<String> newProxies=check(crawlPage(request,pageNum++));
                    util.logInfo(newProxies.size()+" proxies on page "+pageNum);
                    util.logInfo("#proxies "+proxies.size());
                    pageNum=pageNum>20?1:pageNum;
                    for(String proxyString:newProxies){
                        Proxy proxy=new Proxy(proxyString);
                        if(!proxies.offerLast(proxy,500, TimeUnit.MILLISECONDS)){
                            fullFlag=true;
                            break;
                        }
                        fullFlag|=(proxies.size()>maxNum);
                       //crawledProxies.add(proxyString);
                    }
                    //util.print(util.toJsonString(crawledProxies));
                    Thread.sleep(100000);
                }
                synchronized (this){
                    this.wait();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }catch (IOException e){
            util.logSevere("Network Error",e);
            Thread.currentThread().interrupt();
        }
    }


    Proxy getProxy(){
        util.logInfo("#proxies "+proxies.size());
        while(true) {
            Proxy proxy = proxies.pollFirst();
            if (proxy == null) {
                //util.print("return null");
                return null;
            }
            if (proxy.isValid()) {
                proxies.offerFirst(proxy);
                return proxy;
            } else {
                if (proxies.size() < minNum) {
                    synchronized (this) {
                        this.notify();
                    }
                }
            }
        }
    }


}
