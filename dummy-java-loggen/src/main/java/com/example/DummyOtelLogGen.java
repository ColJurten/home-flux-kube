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
        // LOGS
        OtlpGrpcLogRecordExporter logExporter = OtlpGrpcLogRecordExporter.builder()
            .setEndpoint("http://signoz-otel-collector.signoz-vector.svc.cluster.local:4317")
            .build();
        SdkLoggerProvider sdkLoggerProvider = SdkLoggerProvider.builder()
            .addLogRecordProcessor(BatchLogRecordProcessor.builder(logExporter).build())
            .build();
        GlobalLoggerProvider.set(sdkLoggerProvider);
        LoggerProvider loggerProvider = GlobalLoggerProvider.get();
        Logger logger = loggerProvider.loggerBuilder("dummy-java-loggen").build();

        // TRACES
        io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter traceExporter = io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter.builder()
            .setEndpoint("http://signoz-otel-collector.signoz-vector.svc.cluster.local:4317")
            .build();
        io.opentelemetry.sdk.trace.SdkTracerProvider tracerProvider = io.opentelemetry.sdk.trace.SdkTracerProvider.builder()
            .addSpanProcessor(io.opentelemetry.sdk.trace.export.BatchSpanProcessor.builder(traceExporter).build())
            .build();
        io.opentelemetry.api.GlobalOpenTelemetry.set(
            io.opentelemetry.sdk.OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build()
        );
        io.opentelemetry.api.trace.Tracer tracer = io.opentelemetry.api.GlobalOpenTelemetry.getTracer("dummy-java-loggen");

        // METRICS
        io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter metricExporter = io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter.builder()
            .setEndpoint("http://signoz-otel-collector.signoz-vector.svc.cluster.local:4317")
            .build();
        io.opentelemetry.sdk.metrics.SdkMeterProvider meterProvider = io.opentelemetry.sdk.metrics.SdkMeterProvider.builder()
            .registerMetricReader(io.opentelemetry.sdk.metrics.export.PeriodicMetricReader.builder(metricExporter).build())
            .build();
        io.opentelemetry.api.metrics.Meter meter = meterProvider.get("dummy-java-loggen");
        io.opentelemetry.api.metrics.LongCounter requestCounter = meter.counterBuilder("dummy_requests_total")
            .setDescription("Total requests handled")
            .setUnit("1")
            .build();

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


            // TRACE
            io.opentelemetry.api.trace.Span span = tracer.spanBuilder("dummy-span")
                .setAttribute("service.name", service)
                .setAttribute("http.status_code", code)
                .setAttribute("event.duration_ms", dur)
                .startSpan();

            // LOG (with trace/span IDs)
            LogRecordBuilder logRecord = logger.logRecordBuilder()
                .setTimestamp(Instant.now())
                .setSeverity(Severity.valueOf(level))
                .setBody(msg)
                .setAttribute(AttributeKey.stringKey("service.name"), service)
                .setAttribute(AttributeKey.longKey("http.status_code"), (long) code)
                .setAttribute(AttributeKey.longKey("event.duration_ms"), (long) dur)
                .setAttribute(AttributeKey.stringKey("k8s.namespace"), "signoz-vector")
                .setAttribute(AttributeKey.stringKey("k8s.app"), "log-generator")
                .setAttribute(AttributeKey.longKey("seq"), (long) i)
                .setAttribute(AttributeKey.stringKey("trace_id"), span.getSpanContext().getTraceId())
                .setAttribute(AttributeKey.stringKey("span_id"), span.getSpanContext().getSpanId());
            logRecord.emit();

            try {
                Thread.sleep(200);
            } finally {
                span.end();
            }

            // METRIC
            requestCounter.add(1, io.opentelemetry.api.common.Attributes.of(
                AttributeKey.stringKey("service.name"), service,
                AttributeKey.stringKey("k8s.namespace"), "signoz-vector"
            ));

            i++;
        }
    }
}
