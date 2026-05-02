package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;


final class ProfilingMethodInterceptor implements InvocationHandler {

    private final Clock clock;
    private final Object delegate;
    private final ZonedDateTime startTime;
    private final ProfilingState state;

    
    ProfilingMethodInterceptor(Clock clock, Object delegate, ProfilingState state, ZonedDateTime startTime) {
        this.clock = Objects.requireNonNull(clock, "Clock must not be null");
        this.delegate = delegate;
        this.state = state;
        this.startTime = startTime;
    }

    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        boolean isProfiled = method.isAnnotationPresent(Profiled.class);
        return isProfiled ? profiledInvocation(method, args) : standardInvocation(method, args);
    }
    private Object standardInvocation(Method method, Object[] args) throws Throwable {
        try {
            Object result = method.invoke(delegate, args);
            return result;
        }  catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }
    }
    private Object profiledInvocation(Method method, Object[] args) throws Throwable {
        Instant start = clock.instant();

        try {
            Object result = method.invoke(delegate, args);
            return result;
        }  catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }  catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } finally {
            recordInvocation(method, start);
        }
    }
    private void recordInvocation(Method method, Instant start) {
        Duration duration = Duration.between(start, clock.instant());
        long threadId = Thread.currentThread().getId();
        state.record(delegate.getClass(), method, duration, threadId);
    }

}
