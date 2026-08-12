package com.example;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Severity;

import java.time.Instant;
import java.util.Random;

public class DummyOtelLogGen {
    public static void main(String[] args) throws InterruptedException {
        OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
        LoggerProvider loggerProvider = openTelemetry.getLogsBridge();
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
                    .setAttribute("service.name", service)
                    .setAttribute("http.status_code", code)
                    .setAttribute("event.duration_ms", dur)
                    .setAttribute("k8s.namespace", "signoz-test")
                    .setAttribute("k8s.app", "log-generator")
                    .setAttribute("seq", i);
            logRecord.emit();
            i++;
            Thread.sleep(200);
        }
    }
}
