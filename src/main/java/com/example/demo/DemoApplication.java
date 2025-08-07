package com.example.demo;

// --- [추가 1] Logger를 사용하기 위한 import 추가 ---
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @RestController
    class HelloController {

        // --- [추가 2] 로그를 남기기 위한 Logger 객체 생성 ---
        private static final Logger log = LoggerFactory.getLogger(HelloController.class);

        @GetMapping("/")
        public String hello() {
            // --- [추가 3] 원하는 로그 출력 코드 삽입 ---
            // 이 로그들이 이제 kubectl logs 에 나타날 것입니다.
            System.out.println(">>> System.out.println: / endpoint was called!");
            log.info(">>> log.info: Hello endpoint processing request.");
            log.debug(">>> log.debug: This is a debug message."); // DEBUG 레벨 로그

            return "Hello! world!!!!";
        }
    }
}