package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args); 
    }

    @RestController
    class HelloController {

        @GetMapping("/")
        public String hello() {
            // OpenTelemetry span 제거
            return "Hello! world!!!!";
        }
    }
}
