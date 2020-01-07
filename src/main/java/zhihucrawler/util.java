package zhihucrawler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.logging.*;

public class util {
    private static Logger logger=Logger.getLogger("default");

    static void setLogger(Logger logger) {
        util.logger = logger;
    }

    private static ObjectMapper mapper=new ObjectMapper();
    static{
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    private static HashMap<String,String> formatMap= new HashMap<>(32);
    static{
//        String followeeFormat="members/%s/followees?include=data[*].answer_count,articles_count,gender,follower_count,is_followed,is_following,badge[?(type=best_answerer)].topics&offset=0&limit=20";
        String followeeFormat="members/%s/followees?include=data[*]&offset=0&limit=20";
//        String followerFormat="members/%s/followers?include=data[*].answer_count,articles_count,gender,follower_count,is_followed,is_following,badge[?(type=best_answerer)].topics&offset=0&limit=20";
        String followerFormat="members/%s/followers?include=data[*]&offset=0&limit=20";
        String infoFormat="members/%s?include=locations,employments,gender,educations,business,voteup_count,thanked_Count,follower_count,following_count,cover_url,following_topic_count,following_question_count,following_favlists_count,following_columns_count,avatar_hue,answer_count,articles_count,pins_count,question_count,columns_count,commercial_question_count,favorite_count,favorited_count,logs_count,marked_answers_count,marked_answers_text,message_thread_token,account_status,is_active,is_bind_phone,is_force_renamed,is_bind_sina,is_privacy_protected,sina_weibo_url,sina_weibo_name,show_sina_weibo,is_blocking,is_blocked,is_following,is_followed,mutual_followees_count,vote_to_count,vote_from_count,thank_to_count,thank_from_count,thanked_count,description,hosted_live_count,participated_live_count,allow_message,industry_category,org_name,org_homepage,badge[?(type=best_answerer)].topics";
        String questionFormat="members/%s/following-questions?include=data[*].created,answer_count,follower_count,author&offset=0&limit=20";
        String topicFormat = "members/%s/following-topic-contributions?include=data[*].topic.introduction&offset=0&limit=20";
        formatMap.put("follower",followerFormat);
        formatMap.put("followee",followeeFormat);
        formatMap.put("info",infoFormat);
        formatMap.put("topic", topicFormat);
        formatMap.put("question",questionFormat);
    }

    /*
    给定用户和请求类型，返回url，
    * 注： url的offset为0
    * */
    static String getUrl(String user,String type){
        String prefix="https://www.zhihu.com/api/v4/";
        return prefix+String.format(formatMap.get(type),user);
    }




    static void print(String msg){
        System.out.println(msg);
    }

    static String getStackTrace(Throwable e){
        StringBuilder builder = new StringBuilder();
        StackTraceElement[] trace = e.getStackTrace();
        for (StackTraceElement traceElement : trace)
            builder.append("\tat ").append(traceElement).append("\n");
        return builder.toString();
    }

    static void logWarning(String msg, Throwable e){
        logger.log(Level.WARNING, msg+"\n"+e.getMessage()+"\nStackTrace:\n"+getStackTrace(e));
    }

    static void logWarning(String msg){
        logger.log(Level.WARNING, msg);
    }

    static void logSevere(String msg,Throwable e){
        logger.log(Level.SEVERE, msg+"\n"+e.getMessage()+"\nStackTrace:\n"+getStackTrace(e));
    }

    static void logSevere(String msg){
        logger.log(Level.SEVERE, msg);
    }

    static void logInfo(String msg){
        logger.log(Level.INFO, msg);
    }



    static String loadFile(String fileName) throws IOException{
        return new String(Files.readAllBytes(Paths.get(fileName)));
    }

    static void writeFile(String fileName, String content)throws FileNotFoundException {
        util.logInfo("writing "+fileName);
        PrintWriter out = new PrintWriter(fileName);
        out.println(content);
        out.flush();
        out.close();
    }

    static <T> T loadJsonString(String str, Class<T> valueType) throws JsonProcessingException {
        return mapper.readValue(str, valueType);
    }

    static <T> T loadJsonString(String str, TypeReference<T> valueTypeRef) throws JsonProcessingException {
        return mapper.readValue(str, valueTypeRef);
    }

    static String toJsonString(Object o) throws JsonProcessingException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);
    }
}
