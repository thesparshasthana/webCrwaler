package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;


public class ParallelWebCrawlerTask extends RecursiveTask<Boolean> {
    private final Clock clock;
    private final int currentDepth;
    private final Instant deadline;
    private final PageParserFactory parserFactory;
    private final String url;
    private final List<Pattern> urlsIgnored;
    private final ConcurrentSkipListSet<String> urlsVisited;
    private final ConcurrentMap<String, Integer> wordCounts;

    
    public ParallelWebCrawlerTask(String url, Instant deadline, int maxDepth, ConcurrentMap<String, Integer> wordCounts,
            ConcurrentSkipListSet<String> urlsVisited, PageParserFactory parserFactory,
            Clock clock, List<Pattern> ignoredUrls) {
        this.url = url;
        this.deadline = deadline;
        this.currentDepth = maxDepth;
        this.wordCounts = wordCounts;
        this.urlsVisited = urlsVisited;
        this.parserFactory = parserFactory;
        this.clock = clock;
        this.urlsIgnored = ignoredUrls;
    }

    
    @Override
    protected Boolean compute() {
        return Stream.of(url)
                .filter(u -> currentDepth != 0 && clock.instant().isBefore(deadline))
                .filter(u -> urlsIgnored.stream().noneMatch(pattern -> pattern.matcher(u).matches()))
                .filter(u -> !isExcludedByRobotsTxt(u))
                .filter(urlsVisited::add)
                .map(u -> parserFactory.get(u).parse())
                .peek(result -> {
                    result.getWordCounts().forEach((key, value) ->
                            wordCounts.compute(key, (k, v) -> (v == null) ? value : value + v));
                    result.getLinks().stream()
                            .map(link -> new ParallelWebCrawlerTask(link, deadline, currentDepth - 1, wordCounts,
                                    urlsVisited,
                                    parserFactory, clock, urlsIgnored))
                            .map(ParallelWebCrawlerTask::fork)
                            .forEach(ForkJoinTask::invoke);
                })
                .findAny().isPresent();
    }
    private boolean isExcludedByRobotsTxt(String url) {
        try {
            URL robotsTxtUrl = new URL(url + "/robots.txt");
            InputStream inputStream = robotsTxtUrl.openStream();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                Predicate<String> isDisallowedLine = line -> line.startsWith("Disallow:");
                Predicate<String> isExcludedUrl = line -> {
                    String excludedPath = line.substring("Disallow:".length()).trim();
                    return url.endsWith(excludedPath) || url.contains(excludedPath + "/");
                };
                return reader.lines()
                        .map(String::trim)
                        .filter(isDisallowedLine)
                        .anyMatch(isExcludedUrl);

            }
        } catch (IOException e) {
            return false;
        }
    }

}
