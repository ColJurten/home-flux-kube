package com.example;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.GlobalLoggerProvider;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;

import java.time.Instant;
import java.util.Random;

public class DummyOtelLogGen {
    public static void main(String[] args) throws InterruptedException {
        OtlpGrpcLogRecordExporter exporter = OtlpGrpcLogRecordExporter.builder()
            .setEndpoint("http://signoz-otel-collector.signoz-vector.svc.cluster.local:4317")
            .build();
        SdkLoggerProvider sdkLoggerProvider = SdkLoggerProvider.builder()
            .addLogRecordProcessor(BatchLogRecordProcessor.builder(exporter).build())
            .build();
        GlobalLoggerProvider.set(sdkLoggerProvider);
        LoggerProvider loggerProvider = GlobalLoggerProvider.get();
        Logger logger = loggerProvider.loggerBuilder("dummy-java-loggen").build();
        Random random = new Random();
        int i = 0;
        while (true) {
            String[] levels = {"INFO", "WARN", "ERROR"};
            String[] services = {"api", "worker"};
            String level = levels[random.nextInt(levels.length)];
            String service = services[random.nextInt(services.length)];
            int code = 200 + random.nextInt(5) * 10;
            int dur = 10 + random.nextInt(900);
            String msg = level.equals("ERROR") ? "db timeout" : "request handled";
            LogRecordBuilder logRecord = logger.logRecordBuilder()
                .setTimestamp(Instant.now())
                .setSeverity(Severity.valueOf(level))
                .setBody(msg)
                .setAttribute(AttributeKey.stringKey("service.name"), service)
                .setAttribute(AttributeKey.longKey("http.status_code"), (long) code)
                .setAttribute(AttributeKey.longKey("event.duration_ms"), (long) dur)
                .setAttribute(AttributeKey.stringKey("k8s.namespace"), "signoz-vector")
                .setAttribute(AttributeKey.stringKey("k8s.app"), "log-generator")
                .setAttribute(AttributeKey.longKey("seq"), (long) i);
            logRecord.emit();
            i++;
            Thread.sleep(200);
        }
    }
}
