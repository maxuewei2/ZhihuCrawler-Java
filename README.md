# ZhihuCrawler-Java

目前支持进度保存及恢复，免费代理。

## 使用

- 创建文本文件cookies.txt，内容如下
```
a
_zap=4af8afdsfd........=........=......._xsrf=0ca00ab.............=..........=..........=.........1577
b
_zap=4af8afdsfd........=........=......._xsrf=0ca00ab.............=..........=..........=.........1577
c
_zap=4af8afdsfd........=........=......._xsrf=0ca00ab.............=..........=..........=.........1577
d
_zap=4af8afdsfd........=........=......._xsrf=0ca00ab.............=..........=..........=.........1577
```
其中奇数行为 cookie 名，可以随意设置，偶数行是请求 header 中的 cookie 值。    
该文件也可以为空，为空时请求不带 cookie。

- 修改 config.json 可更改设置。    
  其中 parallelRequests 为并行的请求线程数，sleepMills 为每次请求后睡眠的最大时长(毫秒)。    
  调整这两个参数可以改变爬取速度，修改 parallelRequests 将呈对应比例改变速度。
  爬取过快很容易被封。

- state.json 存储当前爬取进度，初始时其中的 toVisit 项应填写少数种子用户。

- 默认数据将存储在 data 文件夹，每个用户一个文件，json 格式。

- 需要 Java 11 编译运行，运行命令
```
java -cp target/zhihu-crawler-1.1.3-jar-with-dependencies.jar  zhihucrawler.ZhihuCrawler config.json zh-crawler.log
```
命令需要两个参数，分别为配置文件名和日志文件名。