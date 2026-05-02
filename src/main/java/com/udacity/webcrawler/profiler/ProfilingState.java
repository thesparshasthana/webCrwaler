package com.udacity.webcrawler.profiler;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


public final class ProfilingState {
    private final Map<String, Map<Long, AtomicLong>> callCountsByThread = new ConcurrentHashMap<>();
    private final Map<String, Duration> totalDurations = new ConcurrentHashMap<>();

    
    public void record(Class<?> callingClass, Method method, Duration elapsed, long threadId) {
        Objects.requireNonNull(callingClass);
        Objects.requireNonNull(method);
        Objects.requireNonNull(elapsed);

        if (elapsed.isNegative()) {
            throw new IllegalArgumentException("negative elapsed time");
        }
        String key = formatMethodCall(callingClass, method);

        callCountsByThread
                .computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(threadId, t -> new AtomicLong())
                .addAndGet(1);

        totalDurations.compute(key, (k, v) -> (v == null) ? elapsed : v.plus(elapsed));
    }

    
    public static String formatMethodCall(Class<?> callingClass, Method method) {
        return String.format("%s#%s", callingClass.getName(), method.getName());
    }

    
    public void write(Writer writer) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        callCountsByThread.forEach((methodCall, threadCounts) -> {
            long totalInvocations = threadCounts.values().stream()
                    .mapToLong(AtomicLong::get)
                    .sum();
            Duration totalDuration = totalDurations.get(methodCall);
            stringBuilder.append(methodCall)
                    .append(" took ")
                    .append(formatDuration(totalDuration))
                    .append(" (called ")
                    .append(totalInvocations)
                    .append(" times)")
                    .append(System.lineSeparator());
            threadCounts.forEach((threadId, invocations) -> {
                Duration threadTotalDuration = totalDurations.get(methodCall);
                Duration threadAverageDuration = threadTotalDuration.dividedBy(invocations.get());

                stringBuilder.append("[Thread ID: ")
                        .append(threadId)
                        .append(" (called ")
                        .append(invocations.get())
                        .append(" times)] - Average duration: ")
                        .append(formatDuration(threadAverageDuration))
                        .append(System.lineSeparator());
            });

            stringBuilder.append(System.lineSeparator());
        });

        writer.write(stringBuilder.toString());
    }

    
    public static String formatDuration(Duration duration) {
        return String.format(
                "%sm %ss %sms", duration.toMinutes(), duration.toSecondsPart(), duration.toMillisPart());
    }
}
