package ru.ifmo.rain.kononov.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

/**
 * Hand-made thread pool and task manager.
 */
public class ParallelMapperImpl implements ParallelMapper {
    private final Queue<Runnable> tasks;
    private final List<Thread> pool;

    /**
     * Constructor for ParallelMapperImpl.
     *
     * @param threads amount of threads
     */
    public ParallelMapperImpl(int threads) {
        ThreadUtils.check(threads);
        tasks = new ArrayDeque<>();
        pool = new ArrayList<>();
        for (int i = 0; i < threads; ++i) {
            ThreadUtils.addWithStart(pool,
                    new Thread(() -> {
                        while (!Thread.interrupted()) {
                            try {
                                pollTask();
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    }));
        }
    }

    private class Results<R> extends AbstractList<R> {
        private final List<R> results;
        private int count;
        private RuntimeException exception;

        private Results(final int size) {
            exception = null;
            results = new ArrayList<>(Collections.nCopies(size, null));
            count = 0;
        }

        synchronized void setException(RuntimeException ex) {
            exception = ex;
        }

        public synchronized R set(final int ind, R res) {
            results.set(ind, res);
            count++;
            if (count == results.size()) {
                notify();
            }
            return null;
        }

        synchronized List<R> get() throws InterruptedException {
            while (count != results.size()) {
                wait();
            }
            if (exception != null)
                throw exception;
            return results;
        }

        @Override
        public R get(int index) {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }
    }

    /**
     * Maps function {@code f} over specified {@code args}.
     * Mapping for each element performs in parallel.
     *
     * @param f    function to map
     * @param args list of arguments for f
     * @throws RuntimeException if calling thread was interrupted
     */
    public <T, R> List<R> map(final Function<? super T, ? extends R> f, final List<? extends T> args) throws InterruptedException {
        final int size = args.size();
        Results<R> results = new Results<>(size);
        for (int i = 0; i < size; ++i) {
            try {
                addTask(ThreadUtils.getRunnable(results, i, f, args));
            } catch (RuntimeException e) {
                results.setException(e);
            }
        }
        return results.get();
    }

    /**
     * Stops all threads. All unfinished mappings leave in undefined state.
     */
    public void close() {
        ThreadUtils.interruptAll(pool);
        ThreadUtils.joinAll(pool);
    }

    private void addTask(final Runnable task) {
        synchronized (tasks) {
            tasks.add(task);
            tasks.notify();
        }
    }

    private void pollTask() throws InterruptedException {
        Runnable task;
        synchronized (tasks) {
            while (tasks.isEmpty()) {
                tasks.wait();
            }
            task = tasks.poll();
        }
        task.run();
    }
}
