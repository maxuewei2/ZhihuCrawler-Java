package zhihucrawler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

public class ZhihuCrawler {
    private Config config;
    private LinkedList<String> writed; //存储已写入的用户集合
    private LinkedHashSet<String> visited;  //存储已写入及已放入request队列的用户
    private LinkedHashSet<String> toVisit;  //存储待访问的用户
    private ConcurrentHashMap<String, String> errorUsers;  //存储出错的用户

    private ZhihuCrawler(String configFileName, String logFileName) throws IOException {
        String content=loadConfig(configFileName);
        util.setLogger(initLogger(logFileName));
        util.logInfo("config: " + content);
    }

    /*读取配置文件*/
    private String loadConfig(String configFileName) throws IOException {
        String content = util.loadFile(configFileName);
        this.config = util.loadJsonString(content, Config.class);
        return content;
    }

    /*读取之前的进度并恢复进度*/
    @SuppressWarnings("unchecked")
    private void loadState() throws IOException {
        String content = util.loadFile(config.stateFileName);
        Map<String, Collection<?>> state = util.loadJsonString(content, new TypeReference<>() {
        });

        /*因visited包含writed和在request队列中的用户，所以上次终止程序时队列中的用户都没写入文件，需要重新请求*/
        visited = new LinkedHashSet<>((List<String>) state.get("visited"));
        writed = new LinkedList<>((List<String>) state.get("writed"));
        visited.removeAll(writed);
        toVisit = new LinkedHashSet<>(visited);
        toVisit.addAll((List<String>) state.get("toVisit"));
        visited.clear();
        visited.addAll(writed);

        List<Map<String, String>> tmp = (List<Map<String, String>>) state.get("errorUsers");
        errorUsers = new ConcurrentHashMap<>(tmp.size() * 3);
        for (Map<String, String> e : tmp) {
            errorUsers.putAll(e);
        }
        util.logInfo("load state" +
                "\n\ttoVisit " + toVisit.size() +
                "\n\tvisited " + visited.size() +
                "\n\twrited " + writed.size() +
                "\n\terrorUsers " + errorUsers.size());
    }

    /*保存当前进度到一个json文件*/
    private void saveState() {
        HashMap<String, Collection<?>> tmp = new HashMap<>();
        tmp.put("toVisit", toVisit);
        tmp.put("visited", visited);
        tmp.put("writed", writed);
        tmp.put("errorUsers", errorUsers.entrySet());
        try {
            String tmpFileName = config.stateFileName + ".tmp";
            util.writeFile(tmpFileName, util.toJsonString(tmp));  //写入临时文件
            //以临时文件覆盖state文件
            Files.move(Paths.get(tmpFileName), Paths.get(config.stateFileName), StandardCopyOption.REPLACE_EXISTING);

            util.logInfo("save state" +
                    "\n\ttoVisit " + toVisit.size() +
                    "\n\tvisited " + visited.size() +
                    "\n\twrited " + writed.size() +
                    "\n\terrorUsers " + errorUsers.size());
            //util.writeFile(config.stateFileName, util.toJsonString(tmp));
        } catch (IOException e) {
            util.logSevere("Failed to write state file.", e);
            //System.exit(-1);
        }
    }


    private void startCrawler() throws IOException {
        loadState();
        /*
        * main -> Requests.requestQueue -> Requests.dataQueue -> writeQueue -> files
        * */
        Requests requests = new Requests(config.cookieFileName, config.parallelRequests, config.tryMax, errorUsers);

        LinkedBlockingQueue<User> writeQueue = new LinkedBlockingQueue<>(config.parallelRequests);

        new ConstructUsers((config.parallelRequests + 50) / 50, requests.getDataQueue(), writeQueue, errorUsers).startThreads();

        requests.startThreads();

        /*监测线程
        * - 定时gc
        * - 定时打印数据情况
        * - 到一定时间退出程序
        * */
        new Thread(() -> {
            int i = 0;
            while (true) {
                try {
                    Thread.sleep(1000);
                    if(requests.aliveRequestNum==0){
                        util.logSevere("no alive request threads left.");
                        cleanBeforeShutdown(this);
                        System.exit(0);
                    }
                    if (i % 30 == 0) {
                        System.gc();
                    }
                    if (i % 5 == 0) {
                        util.logInfo("\n\tvisited " + visited.size() +
                                "\n\ttoVisit " + toVisit.size() +
                                "\n\twrited " + writed.size() +
                                "\n\trequestQueue " + requests.getRequestQueue().size() +
                                "\n\trequestQueue0 " + requests.getRequestQueue0().size() +
                                "\n\tresponseQueue " + requests.getResponseQueue().size() +
                                "\n\tdataQueue " + requests.getDataQueue().size() +
                                "\n\twriteQueue " + writeQueue.size() +
                                "\n\terrorUsers " + errorUsers.size()
                        );
                    }
                    if (i > (3600 * 6)) {
                        util.logInfo("exit after 6 hour.");
                        System.exit(0);
                    }
                    i++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

            }
        }).start();

        /*写数据线程
        * - 从writeQueue取用户数据并写入文件
        * - 将该用户的好友加入toVisit集合
        * - 保存当前进度*/
        new Thread(() -> {
            try {
                Instant last = Instant.now();
                while (true) {
                    User user = writeQueue.take();

                    String fileName = config.dataDirName + "/" + user.userID + ".json";
                    try {
                        util.writeFile(fileName, user.getJsonString());
                    } catch (JsonProcessingException e) {
                        errorUsers.put(user.userID, "JSON PROCESSING 4");
                        util.logWarning("ERRORUSER JSON PROCESSING " + user.userID);
                    } catch (FileNotFoundException e) {
                        errorUsers.put(user.userID, "FILENOTFOUND 5");
                        util.logWarning("ERRORUSER FILENOTFOUND " + user.userID);
                    }

                    List<String> friends = user.getFriends();
                    synchronized (this) {
                        writed.add(user.userID);
                        for (String friend : friends) {
                            if (!visited.contains(friend)) {
                                toVisit.add(friend);
                            }
                        }
                    }
                    util.logInfo("writeUser " + user.userID);
                    synchronized (requests.getRequestQueue()) {
                        requests.getRequestQueue().notify();
                    }
                    friends.clear();
                    user.followers.clear();
                    user.followees.clear();
                    user.topics = null;
                    user.questions = null;
                    user.info = null;

                    if (Duration.between(last, Instant.now()).toSeconds() > 600) {
                        synchronized (this) {
                            saveState();
                        }
                        last = Instant.now();
                    }
                }
            } catch (InterruptedException e) {
                util.logInfo("write interrupted");
                Thread.currentThread().interrupt();
            }
        }).start();

        /*将toVisit中的用户加入Requests的队列*/
        new Thread(()-> {
            try {
            /*for (String user : errorUsers.keySet()) {
                crawlUsers.getToCrawlUsersQueue().put(user);
                errorUsers.remove(user);
            }
            util.logInfo("put errorUsers in toCrawlQueue");*/

                while (true) {
                    LinkedList<String> tmp = new LinkedList<>();
                    synchronized (this) {
                        for (String user : toVisit) {
                            //所有用户都是先请求info数据，然后根据info构造其他数据请求
                            if (requests.getRequestQueue().offer(new RequestNode(user, "info"))) {
                                visited.add(user);
                                tmp.add(user);
                            } else {
                                break;
                            }
                        }
                        toVisit.removeAll(tmp);
                    }
                    synchronized (requests.getRequestQueue()) {
                        requests.getRequestQueue().wait();
                    }
                    tmp.clear();
                }
            } catch (InterruptedException e) {
                util.print("crawler interrupted");
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    Logger initLogger(String logFileName) throws IOException {
        Logger logger = Logger.getLogger("zh-crawler");
        logger.setUseParentHandlers(false); //使不打印到终端

        FileHandler fh = new FileHandler(logFileName, true); //日志append输出到文件
        fh.setFormatter(new SimpleFormatter() {
            private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s %n";

            @Override
            public synchronized String format(LogRecord lr) {
                return String.format(format,
                        new Date(lr.getMillis()),
                        lr.getLevel().getLocalizedName(),
                        lr.getMessage()
                );
            }
        });
        logger.addHandler(fh);
        if (config.verbose) {
            logger.setLevel(Level.ALL);
        } else {
            logger.setLevel(Level.WARNING);
        }
        return logger;
    }

    private static void cleanBeforeShutdown(ZhihuCrawler crawler){
        crawler.saveState();
    }

    public static void main(String[] args) {
        try {
            ZhihuCrawler crawler = new ZhihuCrawler("config.json", "/dev/shm/zh-crawler.log");
            crawler.startCrawler();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                cleanBeforeShutdown(crawler);
                String msg=String.format("System shutdown at %s.", LocalDateTime.now());
                util.print(msg);
                util.logWarning(msg);
            }));
        } catch (IOException e) {
            util.logSevere("IOException", e);
            System.exit(-1);
        }
    }
}
