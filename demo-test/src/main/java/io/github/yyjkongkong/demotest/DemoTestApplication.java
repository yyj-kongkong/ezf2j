package io.github.yyjkongkong.demotest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.ezf2js.autoconfigure.MediaServerAutoConfiguration;
import org.springframework.context.annotation.Import;
@SpringBootApplication
@Import(MediaServerAutoConfiguration.class)
public class DemoTestApplication {

   public static void main(String[] args) {
        SpringApplication.run(DemoTestApplication.class, args);
    }
}
