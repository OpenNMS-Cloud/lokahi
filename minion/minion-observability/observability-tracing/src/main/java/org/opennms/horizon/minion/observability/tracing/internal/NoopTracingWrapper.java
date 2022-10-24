package org.opennms.horizon.minion.observability.tracing.internal;

import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracer;
import io.opentracing.noop.NoopTracerFactory;
import io.opentracing.propagation.Format;

public class NoopTracingWrapper implements Tracer  {

  private final NoopTracer tracer = NoopTracerFactory.create();

  public Tracer init(String s) {
    return tracer;
  }

    @Override
    public ScopeManager scopeManager() {
        return null;
    }

    @Override
    public Span activeSpan() {
        return null;
    }

    @Override
    public SpanBuilder buildSpan(String s) {
        return null;
    }

    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C c) {

    }

    @Override
    public <C> SpanContext extract(Format<C> format, C c) {
        return null;
    }
}
