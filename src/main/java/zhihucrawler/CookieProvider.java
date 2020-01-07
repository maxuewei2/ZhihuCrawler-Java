package zhihucrawler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
class Cookie{
    private String cookieString;
    private String cookieUser;
    Cookie(String cookieUser,String cookieString){
        this.cookieUser=cookieUser;
        this.cookieString=cookieString;
    }

    public String getCookieString() {
        return cookieString;
    }

    @Override
    public String toString() {
        return cookieUser;
    }
}
class CookieProvider {
    private ArrayList<Cookie> cookies;
    private AtomicInteger index=new AtomicInteger(0);

    @SuppressWarnings("unchecked")
    CookieProvider(String cookieFileName)throws IOException {
        cookies=new ArrayList<>();
        Map<String,String> tmp=util.loadJsonString(util.loadFile(cookieFileName),Map.class);
        for (Map.Entry<String,String> e:tmp.entrySet()) {
            cookies.add(new Cookie(e.getKey(),e.getValue()));
        }
    }


    Cookie getCookie(){
        if(cookies.size()==0){
            return null;
        }
        while(true){
            int current=index.get();
            int next=(current+1)%cookies.size();
            if(index.compareAndSet(current,next))
                return cookies.get(next);
        }
    }
}
