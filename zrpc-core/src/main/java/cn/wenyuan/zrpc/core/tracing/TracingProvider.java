package cn.wenyuan.zrpc.core.tracing;


import cn.wenyuan.zrpc.core.config.ApplicationConfig;
import cn.wenyuan.zrpc.core.config.ZrpcConfig;
import brave.Tracer;
import brave.Tracing;
import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import brave.propagation.TraceContext;
import brave.sampler.Sampler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.urlconnection.URLConnectionSender;

/**
 * 管理 Brave Tracer 的单例，支持根据配置启用/禁用 Zipkin 上报。
 */
@Slf4j
public final class TracingProvider {

    private final boolean enabled;
    private final Tracing tracing;
    private final Tracer tracer;
    private final AsyncReporter<zipkin2.Span> reporter;
    private final Sender sender;
    private final TraceContext.Injector<Map<String, String>> injector;
    private final TraceContext.Extractor<Map<String, String>> extractor;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    private TracingProvider() {
        ZrpcConfig config = ApplicationConfig.getConfig();
        ZrpcConfig.TracingConfig tracingConfig = config != null ? config.getTracing() : null;
        boolean enable = tracingConfig != null && tracingConfig.isEnabled();
        if (!enable) {
            log.info("Zipkin tracing disabled via configuration.");
            this.enabled = false;
            this.tracing = null;
            this.tracer = null;
            this.reporter = null;
            this.sender = null;
            this.injector = null;
            this.extractor = null;
            return;
        }

        String serviceName = tracingConfig.getServiceName() != null
            ? tracingConfig.getServiceName()
            : "zrpc-app";
        String zipkinUrl = tracingConfig.getZipkinUrl() != null
            ? tracingConfig.getZipkinUrl()
            : "http://localhost:9411/api/v2/spans";
        double samplerRate = tracingConfig.getSamplerRate() != null
            ? tracingConfig.getSamplerRate()
            : 1.0;

        this.sender = URLConnectionSender.create(zipkinUrl);
        this.reporter = AsyncReporter.create(sender);

        this.tracing = Tracing.newBuilder()
            .localServiceName(serviceName)
            .spanReporter(reporter)
            .sampler(Sampler.create((float) Math.max(0d, Math.min(1d, samplerRate))))
            .addSpanHandler(new SpanHandler() {
                @Override
                public boolean end(TraceContext context, MutableSpan span, Cause cause) {
                    return true;
                }
            })
            .build();
        this.tracer = tracing.tracer();
        this.injector = tracing.propagation().injector(Map::put);
        this.extractor = tracing.propagation().extractor((carrier, key) -> carrier != null ? carrier.get(key) : null);
        this.enabled = true;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                shutdown();
            } catch (Exception ex) {
                log.warn("关闭 Zipkin TracingProvider 时出现异常", ex);
            }
        }));
        log.info("Zipkin tracing enabled, reporting to {}", zipkinUrl);
    }

    private void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            if (reporter != null) {
                reporter.close();
            }
            if (sender != null) {
                try {
                    sender.close();
                } catch (IOException ex) {
                    log.warn("关闭 Zipkin Sender 失败", ex);
                }
            }
            if (tracing != null) {
                tracing.close();
            }
        }
    }

    private static class Holder {
        private static final TracingProvider INSTANCE = new TracingProvider();
    }

    public static TracingProvider getInstance() {
        return Holder.INSTANCE;
    }

    public static boolean isEnabled() {
        return getInstance().enabled;
    }

    public static Tracer tracer() {
        return getInstance().tracer;
    }

    public static TraceContext.Injector<Map<String, String>> injector() {
        return getInstance().injector;
    }

    public static TraceContext.Extractor<Map<String, String>> extractor() {
        return getInstance().extractor;
    }
}
