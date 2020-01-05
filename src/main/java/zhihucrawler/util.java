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
import java.util.logging.*;

public class util {
    private static ObjectMapper mapper=new ObjectMapper();
    private static Logger logger=Logger.getLogger("default");
    static{
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    static void print(String msg){
        System.out.println(msg);
    }

    static void setLogger(Logger logger) {
        util.logger = logger;
    }

    static String getStackTrace(Throwable e){
        StringBuilder builder = new StringBuilder();
        StackTraceElement[] trace = e.getStackTrace();
        for (StackTraceElement traceElement : trace)
            builder.append("\tat ").append(traceElement).append("\n");
        return builder.toString();
    }

    static void logWarning(String msg, Throwable e){
        logger.log(Level.WARNING, msg+"\n"+e.getMessage()+"\n"+getStackTrace(e));
    }

    static void logWarning(String msg){
        logger.log(Level.WARNING, msg);
    }

    static void logSevere(String msg,Throwable e){
        logger.log(Level.SEVERE, msg+"\n"+e.getMessage()+"\n"+getStackTrace(e));
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
