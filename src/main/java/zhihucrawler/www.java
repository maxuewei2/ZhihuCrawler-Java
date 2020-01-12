package zhihucrawler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class www{
    static int fun(int a){

        try{
            util.print("hellow");
            if(a==0){
                return 2;
            }
            return 1;
        }finally {
            util.print("fi");
        }
    }
    public static void main(String [] args) throws JsonProcessingException,InterruptedException, IOException {
        util.print("follower".hashCode()+"");
        util.print("followee".hashCode()+"");
        util.print("topic".hashCode()+"");
        util.print("question".hashCode()%100+"");
        fun(0);
        //ProxyProvider proxyProvider=new ProxyProvider(200);

        Thread.sleep(3000000);

        Thread thread=new Thread(()->{
            int i=0;
            try {
                while (true) {
                    Thread.sleep(100);
                    util.print(i + "");
                    i++;
                }
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
        });
        thread.start();
        Thread.sleep(1000);
        thread.interrupt();
        Thread.sleep(2000);
        util.print(thread.isInterrupted()+"");
        util.print(thread.isAlive()+"");
        System.exit(0);

        HttpClient httpClient;

        HttpClient.Builder clientBuilder=HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2);

        clientBuilder.proxy(ProxySelector.of(new InetSocketAddress("127.0.0.1", 12333)));

        httpClient=clientBuilder.build();


        final String userAgent="Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.79 Safari/537.36";

        //String url="https://www.zhihu.com/api/v4/members/ding-xiang-yi-sheng/followers?include=data[*].answer_count%2Carticles_count%2Cgender%2Cfollower_count%2Cis_followed%2Cis_following%2Cbadge[%3F(type%3Dbest_answerer)].topics&offset=20&limit=20";
        //String url="https://www.zhihu.com/api/v4/members/ding-xiang-yi-sheng/followees?include=data[*].answer_count%2Carticles_count%2Cgender%2Cfollower_count%2Cis_followed%2Cis_following%2Cbadge[%3F(type%3Dbest_answerer)].topics&offset=20&limit=20";
        //String url="https://www.zhihu.com/api/v4/members/ding-xiang-yi-sheng?include=locations,employments,gender,educations,business,voteup_count,thanked_Count,follower_count,following_count,cover_url,following_topic_count,following_question_count,following_favlists_count,following_columns_count,avatar_hue,answer_count,articles_count,pins_count,question_count,columns_count,commercial_question_count,favorite_count,favorited_count,logs_count,marked_answers_count,marked_answers_text,message_thread_token,account_status,is_active,is_bind_phone,is_force_renamed,is_bind_sina,is_privacy_protected,sina_weibo_url,sina_weibo_name,show_sina_weibo,is_blocking,is_blocked,is_following,is_followed,mutual_followees_count,vote_to_count,vote_from_count,thank_to_count,thank_from_count,thanked_count,description,hosted_live_count,participated_live_count,allow_message,industry_category,org_name,org_homepage,badge[?(type=best_answerer)].topics";
        //String url="https://www.zhihu.com/api/v4/members/ding-xiang-yi-sheng/following-questions?include=data[*].created,answer_count,follower_count,author&offset=0&limit=20";
        String url="https://www.zhihu.com/api/v4/members/ding-xiang-yi-sheng/following-topic-contributions?include=data[*].topic.introduction&offset=0&limit=20";
        //String url="https://www.zhihu.com";
        HttpRequest.Builder builder=HttpRequest.newBuilder()
                .GET()
                .timeout(Duration.ofSeconds(10))
                .uri(URI.create(url))
                .setHeader("User-Agent", userAgent);
        HttpRequest request =builder.build();
        try{
            HttpResponse<String> response=httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            util.print(""+response.statusCode());
            util.print(response.body());
        }catch (ConnectException e){
            util.print("ConnectE "+e);
        }catch (HttpTimeoutException e){
            util.print("TimeOut "+e);
        }

    }
}