package zhihucrawler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class Cookie {
    private String cookieString;
    private String cookieUser;
    private String userAgent;

    Cookie(String cookieUser, String cookieString, String userAgent) {
        this.cookieUser = cookieUser;
        this.cookieString = cookieString;
        this.userAgent = userAgent;
    }

    public String getCookieString() {
        return cookieString;
    }

    public String getUserAgent() {
        return userAgent;
    }

    @Override
    public String toString() {
        return cookieUser;
    }
}

class CookieProvider {
    private ArrayList<Cookie> cookies;
    private AtomicInteger index = new AtomicInteger(0);

    @SuppressWarnings("unchecked")
    CookieProvider(String cookieFileName, String userAgentFileName) throws IOException {
        cookies = new ArrayList<>();
        String[] uas = util.loadFile(userAgentFileName).split("\n");  //TODO check content format, eg. blank lines
        String[] cks = util.loadFile(cookieFileName).split("\n");  //TODO check content format, eg. blank lines
        assert cks.length % 2 == 0;
        for (int i = 0; i < cks.length / 2; i++) {
            cookies.add(new Cookie(cks[2 * i], cks[2 * i + 1], uas[i]));
        }
        /*Map<String,String> tmp=util.loadJsonString(util.loadFile(cookieFileName),Map.class);
        int i=0;
        for (Map.Entry<String,String> e:tmp.entrySet()) {
            cookies.add(new Cookie(e.getKey(),e.getValue(),uas[i++]));
        }*/
        util.logInfo("load " + cookies.size() + " cookies.");
    }


    Cookie getCookie() {
        if (cookies.size() == 0) {
            return null;
        }
        while (true) {
            int current = index.get();
            int next = (current + 1) % cookies.size();
            if (index.compareAndSet(current, next))
                return cookies.get(next);
        }
    }
}
