package zhihucrawler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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

    ProxyProvider(int minNum){
        this.minNum=minNum;
        this.maxNum=3*minNum;
        //crawledProxies=new HashSet<>();
        proxies=new LinkedBlockingDeque<>();
        new Thread(this::crawlProxies).start();
    }

    /*解析页面获取代理列表*/
    private ArrayList<String> crawlPage(Request request, int pageNum) throws IOException, InterruptedException {
        final Pattern pattern=Pattern.compile("alt=\"Cn\" /></td>\\s+<td>([^<]+)</td>\\s+<td>([^<]+)</td>");
        String url="https://www.xicidaili.com/wn/"+pageNum;
        HttpResponse<String> response=request.request(url,"");

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

    /*测试代理*/
    boolean test(String proxyString){
        try {
            Proxy proxy=new Proxy(proxyString);
            HttpClient client=HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .proxy(ProxySelector.of(new InetSocketAddress(proxy.getIP(), proxy.getPort())))
                    .build();
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .uri(URI.create("https://www.zhihu.com"));
            client.send(builder.build(), responseInfo -> HttpResponse.BodySubscribers.discarding());
        } catch (InterruptedException e) {
            assert true;
        } catch (IOException e) {
            util.logWarning("Request " + proxyString + " " + e.getMessage());
            return false;
        }
        return true;
    }

    /*测试代理列表，返回其中有效的代理*/
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

    private void loadProxies(){
        try {
            Pattern pattern = Pattern.compile("(?:\\d{1,3}\\.){3}\\d{1,3}:\\d+");
            String content = util.loadFile("proxies.txt");
            String[] tmp = content.split("\n");
            int i = 0;
            while (i < tmp.length) {
                ArrayList<String> pros = new ArrayList<>();
                for (int j = 0; i < tmp.length && j < 200; j++, i++) {
                    Matcher matcher = pattern.matcher(tmp[i]);
                    if (matcher.find()) {
                        pros.add(matcher.group());
                    }
                }
                ArrayList<String> validPros = check(pros);
                for (String pro : validPros) {
                    proxies.put(new Proxy(pro));
                    if (proxies.size() > maxNum) {
                        synchronized (this) {
                            this.wait();
                        }
                    }
                }
                Thread.sleep(1000);
            }
            util.print("#proxies after load file "+proxies.size());
        }catch (IOException e){

        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

    /*循环爬取代理网站1-20页，直到proxies中代理数量大于maxNum*/
    @SuppressWarnings("unchecked")
    private void crawlProxies(){
        Request request=new Request(null,null,3);
        //request.setProxy(new Proxy("127.0.0.1:12333"));
        try {
            loadProxies();
            //request.changeProxy();
            while(true){
                boolean fullFlag=false;
                int pageNum=1;
                while(!fullFlag){
                    ArrayList<String> newProxies=check(crawlPage(request,pageNum++));
                    util.logInfo(newProxies.size()+" proxies on page "+pageNum);
                    util.logInfo("#proxies "+proxies.size());
                    pageNum=pageNum>20?1:pageNum;
                    for(int i=0;i<newProxies.size()&&!fullFlag;i++){
                        Proxy proxy=new Proxy(newProxies.get(i));
                        fullFlag=(!proxies.offerLast(proxy,500, TimeUnit.MILLISECONDS))|(proxies.size()>maxNum);
                       //crawledProxies.add(proxyString);
                    }
                    //util.print(util.toJsonString(crawledProxies));
                    Thread.sleep(200000);
                }
                synchronized (this){
                    this.wait();
                }
            }
        } catch (InterruptedException e) {
            util.logWarning("ProxyProvider crawlProxies Interrupted",e);
            Thread.currentThread().interrupt();
        }catch (IOException e){
            util.logSevere("ProxyProvider Network Error",e);
            Thread.currentThread().interrupt();
        }
    }


    /*获取代理，遇到失效代理则丢弃，否则放回队列*/
    Proxy getProxy(){
        util.logInfo("#proxies "+proxies.size());
        while(true) {
            Proxy proxy = proxies.pollFirst();
            if (proxy == null) {
                //util.print("return null");
                return null;
            }
            if (proxy.isValid()) {
                proxies.offerLast(proxy);
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
