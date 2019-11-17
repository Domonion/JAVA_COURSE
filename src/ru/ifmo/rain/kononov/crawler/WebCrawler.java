package ru.ifmo.rain.kononov.crawler;

import java.util.*;
import java.util.concurrent.*;

import java.io.IOException;
import java.nio.file.Paths;

import info.kgeorgiy.java.advanced.crawler.Crawler;
import info.kgeorgiy.java.advanced.crawler.Result;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.CachingDownloader;
import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.URLUtils;

public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final ExecutorService downloadersPool;
    private final ExecutorService extractorsPool;
    private final int perHost;
    private final IOException defaultValue = new IOException();

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        downloadersPool = Executors.newFixedThreadPool(downloaders);
        extractorsPool = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
    }

    public Result download(final String url, int depth) {
        if (depth < 1) {
            throw new IllegalArgumentException("Depth value must be positive integer");
        }
        final List<String> urls = new ArrayList<>();
        final Map<String, IOException> errors = new ConcurrentHashMap<>();
        final Map<String, HostHandler> hosts = new ConcurrentHashMap<>();
        Set<String> vertexes = new ConcurrentSkipListSet<>();
        vertexes.add(url);
        final Phaser sync = new Phaser(1);
        while (depth > 0) {
            final Set<String> nowVertexes = vertexes;
            vertexes = new ConcurrentSkipListSet<>();
            for (String nowUrl : nowVertexes) {
                go(nowUrl, errors, hosts, sync, vertexes);
            }
            sync.arriveAndAwaitAdvance();
            depth--;
        }
        errors.entrySet().stream()
                .filter(entry -> entry.getValue() == defaultValue)
                .map(Map.Entry::getKey)
                .forEach(urls::add);
        errors.values().removeIf(e -> e == defaultValue);
        return new Result(urls, errors);
    }

    public void close() {
        try {
            downloadersPool.shutdown();
            extractorsPool.shutdown();
            downloadersPool.awaitTermination(10, TimeUnit.SECONDS);
            extractorsPool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    private class HostHandler {
        private final Queue<Runnable> queue = new ArrayDeque<>();
        private int count;
        private final Phaser sync;

        HostHandler(Phaser sync) {
            this.sync = sync;
            this.count = 0;
        }

        void submit(Runnable task) {
            sync.register();
            downloadersPool.submit(() -> {
                try {
                    task.run();
                } finally {
                    done();
                    sync.arriveAndDeregister();
                }
            });
        }

        synchronized void add(Runnable task) {
            if (count < perHost) {
                submit(task);
                count++;
            } else {
                queue.add(task);
            }
        }

        private synchronized void done() {
            if (!queue.isEmpty()) {
                submit(queue.poll());
            } else {
                count--;
            }
        }
    }

    private void go(final String url, Map<String, IOException> errors,
                    final Map<String, HostHandler> hosts,
                    final Phaser sync, final Set<String> newUrls) {
        if (errors.putIfAbsent(url, defaultValue) != null) {
            return;
        }
        String host;
        try {
            host = URLUtils.getHost(url);
        } catch (IOException e) {
            errors.put(url, e);
            return;
        }
        hosts.computeIfAbsent(host, s -> new HostHandler(sync)).add(() -> {
            try {
                final Document doc = downloader.download(url);
                sync.register();
                extractorsPool.submit(() -> {
                    try {
                        newUrls.addAll(doc.extractLinks());
                    } catch (IOException e) {
                        errors.put(url, e);
                    } finally {
                        sync.arriveAndDeregister();
                    }
                });
            } catch (IOException e) {
                errors.put(url, e);
            }
        });
    }

    public static void main(String[] args) {
        if (args == null || args.length != 5) {
            System.out.println("Usage: WebCrawler url [depth [downloaders [extractors [perHost]]]]");
            return;
        }
        final String url = args[0];
        final int depth = Integer.parseInt(args[1]);
        final int downloaders = Integer.parseInt(args[2]);
        final int extractors = Integer.parseInt(args[3]);
        final int perHost = Integer.parseInt(args[4]);
        try (Crawler crawler = new WebCrawler(new CachingDownloader(Paths.get(url)), downloaders, extractors, perHost)) {
            Result result = crawler.download(url, depth);
            System.out.println("Downloaded OK:");
            for (String link : result.getDownloaded()) {
                System.out.println(link);
            }
            System.out.println("Errors:");
            for (Map.Entry<String, IOException> entry : result.getErrors().entrySet()) {
                System.out.println(entry.getValue() + " " + entry.getKey());
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println(e.getMessage());
        }
    }
}
