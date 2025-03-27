package com.hktv.ars.config;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

import java.util.concurrent.Executor;

public class ContextPropagatingExecutor implements Executor {

    private final Executor delegate;
    private final Tracer tracer;

    public ContextPropagatingExecutor(Executor delegate, Tracer tracer) {
        this.delegate = delegate;
        this.tracer = tracer;
    }

    @Override
    public void execute(Runnable command) {
        Span currentSpan = tracer.currentSpan();

        delegate.execute(() -> {
            if (currentSpan != null) {
                Span newSpan = tracer.nextSpan(currentSpan).name("async-child-span").start();
                try (Tracer.SpanInScope spanInScope = tracer.withSpan(newSpan)) {
                    command.run();
                } finally {
                    newSpan.end();
                }
            } else {
                command.run();
            }
        });
    }
}
