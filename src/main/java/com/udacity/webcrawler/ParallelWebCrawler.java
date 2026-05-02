package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Pattern;


public final class ParallelWebCrawler implements WebCrawler {
    private final Clock clock;
    private final List<Pattern> ignoredUrls;
    private final int maxDepth;
    private final PageParserFactory parserFactory;
    private final ForkJoinPool pool;
    private final int popularWordCount;
    private final Duration timeout;

    
    @Inject
    public ParallelWebCrawler(
            Clock clock,
            @Timeout Duration timeout,
            @PopularWordCount int popularWordCount,
            @TargetParallelism int threadCount,
            @IgnoredUrls List<Pattern> ignoredUrls,
            @MaxDepth int maxDepth,
            PageParserFactory parserFactory) {
        this.clock = clock;
        this.timeout = timeout;
        this.popularWordCount = popularWordCount;
        this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
        this.ignoredUrls = ignoredUrls;
        this.maxDepth = maxDepth;
        this.parserFactory = parserFactory;
    }

    
    @Override
    public CrawlResult crawl(List<String> startingUrls) {
        Instant deadline = clock.instant().plus(timeout);
        ConcurrentMap<String, Integer> wordCounts = new ConcurrentHashMap<>();
        ConcurrentSkipListSet<String> visitedUrls = new ConcurrentSkipListSet<>();
        startingUrls.forEach(url -> pool.invoke(new ParallelWebCrawlerTask(url, deadline, maxDepth, wordCounts, visitedUrls,
                        parserFactory, clock, ignoredUrls)));
        return wordCounts.isEmpty()
                ? new CrawlResult.Builder()
                .setWordCounts(wordCounts)
                .setUrlsVisited(visitedUrls.size())
                .build()
                : new CrawlResult.Builder()
                .setWordCounts(WordCounts.sort(wordCounts, popularWordCount))
                .setUrlsVisited(visitedUrls.size())
                .build();

    }

    
    @Override
    public int getMaxParallelism() {
        return Runtime.getRuntime().availableProcessors();
    }
}
