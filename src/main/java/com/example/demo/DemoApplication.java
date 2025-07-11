package com.example.demo;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        initOpenTelemetry(); // ✅ OTEL 초기화
        SpringApplication.run(DemoApplication.class, args);
    }

    @RestController
    class HelloController {

        private final Tracer tracer = GlobalOpenTelemetry.getTracer("demo-service");

        @GetMapping("/")
        public String hello() {
            Span span = tracer.spanBuilder("hello-handler").startSpan();
            try {
                span.setAttribute("custom.message", "Hello from OpenTelemetry!");
                return "Hello! world!!!!";
            } finally {
                span.end(); // ✅ Span 종료
            }
        }
    }

    // ✅ OTLP Exporter 초기화
    private static void initOpenTelemetry() {
        // OTLP Exporter 설정
        OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint("http://tempo.prometheus.svc.cluster.local:4317") // Tempo 주소
                .setTimeout(Duration.ofSeconds(5))
                .build();

        // Resource 설정 (서비스 이름)
        Resource resource = Resource.getDefault().merge(
                Resource.create(Attributes.of(
                        ResourceAttributes.SERVICE_NAME, "demo-service"
                ))
        );

        // TracerProvider 및 Processor 설정
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .build();

        // Global 등록
        OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();
    }
}
