package zhihucrawler;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class PutUserRequests {
    private static HashMap<String,String> formatMap= new HashMap<>(32);
    static{
        String followeeFormat="members/%s/followees?include=data[*].answer_count,articles_count,gender,follower_count,is_followed,is_following,badge[?(type=best_answerer)].topics&offset=0&limit=20";
        //String followeeFormat="members/%s/followees?include=data[*]&offset=0&limit=20";
        String followerFormat="members/%s/followers?include=data[*].answer_count,articles_count,gender,follower_count,is_followed,is_following,badge[?(type=best_answerer)].topics&offset=0&limit=20";
        //String followerFormat="members/%s/followers?include=data[*]&offset=0&limit=20";
        String infoFormat="members/%s?include=locations,employments,gender,educations,business,voteup_count,thanked_Count,follower_count,following_count,cover_url,following_topic_count,following_question_count,following_favlists_count,following_columns_count,avatar_hue,answer_count,articles_count,pins_count,question_count,columns_count,commercial_question_count,favorite_count,favorited_count,logs_count,marked_answers_count,marked_answers_text,message_thread_token,account_status,is_active,is_bind_phone,is_force_renamed,is_bind_sina,is_privacy_protected,sina_weibo_url,sina_weibo_name,show_sina_weibo,is_blocking,is_blocked,is_following,is_followed,mutual_followees_count,vote_to_count,vote_from_count,thank_to_count,thank_from_count,thanked_count,description,hosted_live_count,participated_live_count,allow_message,industry_category,org_name,org_homepage,badge[?(type=best_answerer)].topics";
        String questionFormat="members/%s/following-questions?include=data[*].created,answer_count,follower_count,author&offset=0&limit=20";
        String topicFormat = "members/%s/following-topic-contributions?include=data[*].topic.introduction&offset=0&limit=20";
        formatMap.put("follower",followerFormat);
        formatMap.put("followee",followeeFormat);
        formatMap.put("info",infoFormat);
        formatMap.put("topic", topicFormat);
        formatMap.put("question",questionFormat);
    }

    private static String getUrl(String type,String user){
        String prefix="https://www.zhihu.com/api/v4/";
        return prefix+String.format(formatMap.get(type),user);
    }


    private final BlockingQueue<String> toCrawlUsersQueue;
    private final BlockingQueue<RequestNode> requestQueue;
    private final int workers;
    PutUserRequests(int workers,
                    BlockingQueue<RequestNode> requestQueue){
        this.workers=workers;
        this.toCrawlUsersQueue= new LinkedBlockingQueue<>(100);
        this.requestQueue=requestQueue;
    }

    BlockingQueue<String> getToCrawlUsersQueue() {
        return toCrawlUsersQueue;
    }

    private void doTask() {
        String []types={"follower","followee","info","topic","question"};
        while(true){
            try {
                String user=toCrawlUsersQueue.poll(10000, TimeUnit.MILLISECONDS);
                synchronized (toCrawlUsersQueue) {
                    toCrawlUsersQueue.notify();
                }
                if(user==null){
                    continue;
                }
                for (String type:types) {
                    String url = getUrl(type, user);
                    RequestNode requestNode=new RequestNode(user,type,url,0,-1,0);
                    requestQueue.put(requestNode);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    void startThreads() {
        for (int i = 0; i < workers; i++) {
            new Thread(this::doTask).start();
        }
    }

}
