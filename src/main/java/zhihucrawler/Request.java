package zhihucrawler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

    void changeProxy(){
        changeProxyFlag=true;
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
        for (int i = 0; i < tryMax; i++) {
            try {
                return sendGet(url);
            } catch (IOException e) {
                assert true;
            }
        }
        throw new IOException();
    }

    HttpResponse<String> request(String url) throws InterruptedException,IOException {
        if(proxyProvider!=null&&Duration.between(last,Instant.now()).toSeconds()>600){
            setProxy(proxyProvider.getProxy());
            last=Instant.now();
        }
        if(proxyProvider!=null&&changeProxyFlag){
            setProxy(proxyProvider.getProxy());
            changeProxyFlag=false;
        }
        if(proxyProvider!=null&&proxy==null){
            Proxy tmp=proxyProvider.getProxy();
            if(tmp!=null){
                setProxy(tmp);
            }
        }

        try {
            return sendRequest(url);
        }catch (IOException e){
            if(proxy==null){
                throw new IOException("Network Error");
            }
            if(proxyProvider!=null) {
                proxy.setError();
                proxy = proxyProvider.getProxy();
                setProxy(proxy);
            }
            throw new IOException("Proxy Network Error");
        }
    }

}
