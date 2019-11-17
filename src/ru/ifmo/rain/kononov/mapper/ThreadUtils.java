package ru.ifmo.rain.kononov.mapper;

import java.util.AbstractList;
import java.util.List;
import java.util.function.Function;

class ThreadUtils {
    static <T, R> Runnable getRunnable(AbstractList<R> results, final int ind, final Function<? super T, ? extends R> f, final List<? extends T> arg) {
        return () -> results.set(ind, f.apply(arg.get(ind)));
    }

    static void check(final int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("Number of threads must be positive");
        }
    }

    static void addWithStart(final List<Thread> threads, final Thread thread) {
        threads.add(thread);
        thread.start();
    }

    static void interruptAll(final List<Thread> threads) {
        for (Thread thread : threads) {
            thread.interrupt();
        }
    }

    static void joinAll(final List<Thread> threads) {
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }
}
