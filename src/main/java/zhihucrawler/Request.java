package zhihucrawler;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;


class Request {
    private final CookieProvider cookieProvider;
    private final ProxyProvider proxyProvider;
    private final int tryMax;
    private HttpClient httpClient;
    private Proxy proxy;
    private Instant last=Instant.now();
    private volatile boolean changeProxyFlag=false;
    private final boolean verbose;

    Request(CookieProvider cookieProvider,ProxyProvider proxyProvider,int tryMax,boolean verbose){
        this.cookieProvider=cookieProvider;
        this.proxyProvider=proxyProvider;
        this.tryMax = tryMax;
        this.verbose=verbose;
        if(proxyProvider!=null){
            setProxy(proxyProvider.getProxy());
        }else{
            setProxy(null);
        }
    }

    Proxy getProxy() {
        return proxy;
    }

    void setChangeProxyFlag(){
        changeProxyFlag=true;
    }

    void changeProxy(){
        setProxy(proxyProvider.getProxy());
    }

    void setProxy(Proxy proxy){
        this.proxy=proxy;
        HttpClient.Builder clientBuilder=HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2);
        if(proxy!=null){
            clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(proxy.getIP(), proxy.getPort())));
        }
        httpClient=clientBuilder.build();
    }

    private HttpResponse<String> sendGet(String url) throws IOException, InterruptedException {
        final String userAgent="Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.79 Safari/537.36";

        HttpRequest.Builder builder=HttpRequest.newBuilder()
                .GET()
                .timeout(Duration.ofSeconds(5))
                .uri(URI.create(url))
                .setHeader("User-Agent", userAgent);
        if(cookieProvider!=null) {
            Cookie cookie = cookieProvider.getCookie();
            if (cookie != null) {
                builder.setHeader("Cookie", cookie.getCookieString());
            }
            if(verbose){
                util.logInfo(Thread.currentThread().getName()+" using "+cookie+" "+proxy+" getting " + url);
            }
        }
        HttpRequest request =builder.build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendRequest(String url) throws InterruptedException,IOException {
        Throwable throwable=null;
        for (int i = 0; i < tryMax; i++) {
            try {
                return sendGet(url);
            } catch (IOException e) {
                throwable=e;
            }
        }
        throw new IOException(throwable);
    }

    HttpResponse<String> request(String url,Proxy pro) throws InterruptedException,IOException {
        try {
            return sendRequest(url);
        }catch (IOException throwable){
            String msg=throwable.getMessage();
            /*if(throwable instanceof HttpConnectTimeoutException){
                util.logWarning("HTTPException ConnectTimeOut "+pro+" "+msg);
            }else if(throwable instanceof HttpTimeoutException){
                util.logWarning("HTTPException TimeOut "+pro+" "+msg);
            }else if(throwable instanceof ConnectException){
                util.logWarning("HTTPException  "+pro+" "+msg);
            }*/
            util.logWarning("Request "+pro+" "+msg);
            if(pro==null){
                throw new IOException("Network Error",throwable);
            }
            pro.setError();
            if(proxyProvider!=null) {
                changeProxy();
            }
            throw new IOException("Proxy Network Error",throwable);
        }
    }

    HttpResponse<String> request(String url) throws InterruptedException,IOException {
        if(proxyProvider!=null&&Duration.between(last,Instant.now()).toSeconds()>600){
            changeProxy();
            last=Instant.now();
        }
        if(proxyProvider!=null&&changeProxyFlag){
            changeProxy();
            changeProxyFlag=false;
        }
        if(proxyProvider!=null&&proxy==null){
            Proxy tmp=proxyProvider.getProxy();
            if(tmp!=null){
                setProxy(tmp);
            }
        }
        return request(url,proxy);
    }

}
