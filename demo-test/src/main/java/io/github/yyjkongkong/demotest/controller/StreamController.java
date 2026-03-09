package io.github.yyjkongkong.demotest.controller;

import com.ez2fj.model.Camera;
import com.ez2fj.service.CameraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StreamController {
	
    @Autowired
   private CameraService cameraService;
    
    @GetMapping("/start")
   public String startStream(@RequestParam(defaultValue = "rtsp://admin:admin@192.168.0.101:554/stream") String url) {
        try {
            Camera camera = new Camera();
            camera.setUrl(url);
            camera.setAutoClose(true);
            camera.setNoClientsDuration(60000);
            
            String mediaKey = cameraService.addCamera(camera);
            return "✅ 推流启动成功！\n" +
                   "MediaKey: " + mediaKey + "\n" +
                   "HTTP-FLV: http://localhost:8866/live?url=" + url + "\n" +
                   "WS-FLV: ws://localhost:8866/live?url=" + url;
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 推流启动失败：" + e.getMessage();
        }
    }
    
    @GetMapping("/stop")
   public String stopStream(@RequestParam String url) {
        try {
            boolean removed = cameraService.removeCamera(url);
            return removed ? "✅ 已停止推流" : "⚠️ 未找到该流";
        } catch (Exception e) {
            return "❌ 停止失败：" + e.getMessage();
        }
    }
    
    @GetMapping("/list")
   public String listStreams() {
        int count = cameraService.getCameraCount();
        return "📊 当前活跃流数量：" + count;
    }
    
    @GetMapping("/test")
   public String test() {
        return "✅ 服务运行正常！\n" +
               "访问 /api/start 开始推流\n" +
               "访问 /api/stop?url=xxx 停止推流\n" +
               "访问 /api/list 查看流列表";
    }
}
