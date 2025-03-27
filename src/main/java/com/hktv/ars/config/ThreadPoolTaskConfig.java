package com.hktv.ars.config;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@Configuration
public class ThreadPoolTaskConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(ThreadPoolTaskConfig.class);
    private final Tracer tracer;


    @Value("${ars.async.executor.thread.core_pool_size}")
    private int corePoolSize;

    @Value("${ars.async.executor.thread.max_pool_size}")
    private int maxPoolSize;

    @Value("${ars.async.executor.thread.keep_alive_time}")
    private int keepAliveTime;

    @Value("${ars.async.executor.thread.queue_capacity}")
    private int queueCapacity;

    @Value("${ars.async.executor.thread.name.prefix}")
    private String namePrefix;

    public ThreadPoolTaskConfig(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public Executor getAsyncExecutor() {
        return asyncServiceExecutor();
    }

    @Bean(name = "asyncServiceExecutor")
    public Executor asyncServiceExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setKeepAliveSeconds(keepAliveTime);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(namePrefix);
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return new ContextPropagatingExecutor(executor, tracer);
    }

    public final class MdcTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(final Runnable runnable) {
            final var mdcContextMap = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    if (mdcContextMap != null) {
                        MDC.setContextMap(mdcContextMap);
                    }
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        }
    }


    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            String traceId = tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : "No Trace";
            log.error("Exception in async method: {} with params: {}. TraceId: {}. Exception: {}",
                    method.getName(), params, traceId, throwable.getMessage(), throwable);
        };
    }
}
