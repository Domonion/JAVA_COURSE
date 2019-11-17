package ru.ifmo.rain.kononov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * List iterative parallelism support.
 */
public class IterativeParallelism implements ListIP {

    private <T, R> List<R> resolve(int allowedThreads, final List<? extends T> values, final Function<Stream<? extends T>, ? extends R> task) throws InterruptedException {
        final int size = values.size();
        final int usingThreads = Math.min(allowedThreads, size);
        List<Thread> threads = new ArrayList<>(Collections.nCopies(usingThreads, null));
        List<R> result = new LinkedList<>(Collections.nCopies(usingThreads, null));
        final int block = size / usingThreads;
        final int tail = size % usingThreads;
        int l, r = 0;
        for (int i = 0; i < usingThreads; ++i) {
            l = r;
            r = l + block + (i < tail ? 1 : 0);
            final int l2 = l;
            final int r2 = r;
            final int i2 = i;
            threads.set(i, new Thread(() -> result.set(i2, task.apply(values.subList(l2, r2).stream()))));
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        return result;
    }

    private <T, R, U> R apply(int threads, List<? extends T> values, Function<Stream<? extends T>, ? extends U> task, Function<Stream<? extends U>, R> accumulator) throws InterruptedException {
        return accumulator.apply(resolve(threads, values, task).stream());
    }

    /**
     * Returns maximum value.
     *
     * @param threads    number or concurrent threads.
     * @param values     values to get maximum of.
     * @param comparator value comparator.
     * @param <T>        value type.
     * @return maximum of given values
     * @throws InterruptedException             if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if not values are given.
     */
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException, NoSuchElementException {
        if (values.isEmpty()) {
            throw new NoSuchElementException("List of values is empty");
        }
        Function<Stream<? extends T>, ? extends T> task = stream -> stream.max(comparator).get();
        return apply(threads, values, task, task);
    }

    /**
     * Returns minimum value.
     *
     * @param threads    number or concurrent threads.
     * @param values     values to get minimum of.
     * @param comparator value comparator.
     * @param <T>        value type.
     * @return minimum of given values
     * @throws InterruptedException             if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if not values are given.
     */
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, Collections.reverseOrder(comparator));
    }

    /**
     * Returns whether all values satisfies predicate.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       value type.
     * @return whether all values satisfies predicate or {@code true}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return apply(threads, values,
                stream -> stream.allMatch(predicate),
                stream -> stream.allMatch(Boolean::booleanValue));
    }

    /**
     * Returns whether any of values satisfies predicate.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       value type.
     * @return whether any value satisfies predicate or {@code false}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values,
                e -> !predicate.test(e));
    }

    /**
     * Join values to string.
     *
     * @param threads number of concurrent threads.
     * @param values  values to join.
     * @return list of joined result of {@link #toString()} call on each value.
     * @throws InterruptedException if executing thread was interrupted.
     */
    public String join(int threads, List<?> values) throws InterruptedException {
        return apply(threads, values,
                stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining()));
    }

    /**
     * Filters values by predicate.
     *
     * @param threads   number of concurrent threads.
     * @param values    values to filter.
     * @param predicate filter predicate.
     * @return list of values satisfying given predicated. Order of values is preserved.
     * @throws InterruptedException if executing thread was interrupted.
     */
    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return apply(threads, values,
                stream -> stream.filter(predicate).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    /**
     * Maps values.
     *
     * @param threads number of concurrent threads.
     * @param values  values to map.
     * @param f       mapper function.
     * @return list of values mapped by given function.
     * @throws InterruptedException if executing thread was interrupted.
     */
    public <T, U> List<U> map(final int threads, final List<? extends T> values, final Function<? super T, ? extends U> f) throws InterruptedException {
        return apply(threads, values,
                stream -> stream.map(f).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()));
    }
}
