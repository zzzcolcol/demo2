package com.example.demo;

// Logger를 사용하기 위한 import 추가
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

        // 로그를 남기기 위한 Logger 객체 생성
        private static final Logger log = LoggerFactory.getLogger(HelloController.class);

        @GetMapping("/")
        public String hello() {
            // 기존에 잘 동작하던 엔드포인트
            log.info(">>> log.info: / endpoint was called successfully.");
            return "Hello! world!!!!";
        }
        
        // --- [추가] 에러 발생을 위한 새로운 테스트 엔드포인트 ---
        @GetMapping("/error")
        public String makeError() {
            log.info(">>> log.info: /error endpoint called. Intentionally causing an error...");
            try {
                // 고의로 NullPointerException 발생시키기
                String nullString = null;
                nullString.length(); // 이 라인에서 에러가 발생합니다.
            } catch (Exception e) {
                // 에러를 로그에 기록합니다. 스택 트레이스가 포함됩니다.
                log.error(">>> log.error: An exception occurred!", e);
                // 에러를 다시 던져서 Spring Boot의 기본 에러 핸들러가 처리하도록 합니다.
                throw e;
            }
            return "You should not see this message.";
        }
    }
}