# 🎬 ezf2j
> 轻量级、高性能 Java 流媒体处理框架 | 基于 FFmpeg & JavaCV | Spring Boot 开箱即用

[![Maven Central](https://img.shields.io/maven-central/v/io.github.yyj-kongkong/ezf2j-parent?style=flat-square)](https://central.sonatype.com/artifact/io.github.yyj-kongkong/ezf2j-parent)
[![GitHub Stars](https://img.shields.io/github/stars/yyj-kongkong/ezf2j?style=flat-square)](https://github.com/yyj-kongkong/ezf2j)
[![License](https://img.shields.io/github/license/yyj-kongkong/ezf2j?style=flat-square)](https://github.com/yyj-kongkong/ezf2j/blob/main/LICENSE)
[![Java Version](https://img.shields.io/badge/Java-21+-brightgreen?style=flat-square)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-6DB33F?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)

---

## ✨ 核心特性
- **极简接入**：Spring Boot Starter 一键集成，零配置启动
- **全平台兼容**：Windows / Linux x86_64 原生 FFmpeg 支持
- **高性能**：基于 Netty 异步架构，低延迟、高吞吐
- **功能丰富**：RTSP/RTMP 拉流、转码、推流、截图、录制
- **开箱即用**：内置常用工具类，无需重复造轮子

## 📦 快速开始
### 1. 引入依赖（Maven）
#### //spring boot
```xml
<!--//spring boot-->
<dependency>
    <groupId>io.github.yyj-kongkong</groupId>
    <artifactId>ezf2j-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```
```java
@SpringBootApplication
@Import(MediaServerAutoConfiguration.class);
@Autowired
private CameraService cameraService;

```
#### //其他框架
```xml
<!-- //其他框架-->
<dependency>
<groupId>io.github.yyj-kongkong</groupId>
<artifactId>ezf2j-core</artifactId>
<version>1.0.0</version>
</dependency>
```
```java
//其他框架
// 方式 1：使用默认配置
//EZF2JEngine engine = EZF2JEngine.getInstance();
// 方式 2：使用自定义配置
EZF2JEngine engine = EZF2JEngine.init(InitConfig.builder().port(8787).build());
CameraService cameraService = engine.getCameraService();
Camera camera = new Camera();
 camera.setUrl("rtsp://admin:admin@192.168.0.101:554/stream");
String s = cameraService.addCamera(camera);
// 关闭服务
// engine.shutdown();

```
![log.jpeg](log.jpeg)