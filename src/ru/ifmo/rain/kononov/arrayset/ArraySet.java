package ru.ifmo.rain.kononov.arrayset;


import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {

    private final Comparator<? super T> comparator;
    private final List<T> list;

    private ArraySet(List<T> list, Comparator<? super T> comparator) {
        this.comparator = comparator;
        this.list = list;
    }

    private int getIndex(T element) {
        return Collections.binarySearch(list, element, comparator);
    }

    private int getPosition(T element, boolean greater, boolean strict) {
        int index = getIndex(element);
        if (index < 0)
            return -(index + 1) - (greater ? 0 : 1);
        return index + (strict ? (greater ? +1 : -1) : 0);
    }

    private T getElement(int index) {
        if (-1 < index && index < list.size()) {
            return list.get(index);
        }
        throw new NoSuchElementException("Attempt to access an element with index: " + index + ", which is not in range [" + 0 + ", " + size() + ")");
    }

    private T getElementNonThrow(int index) {
        try {
            return getElement(index);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public ArraySet() {
        this.comparator = null;
        list = Collections.emptyList();
    }

    public ArraySet(Comparator<? super T> comparator) {
        this.comparator = comparator;
        list = Collections.emptyList();
    }

    public ArraySet(Collection<? extends T> coll) {
        this.comparator = null;
        list = List.copyOf(new TreeSet<>(coll));
    }

    public ArraySet(Collection<? extends T> coll, Comparator<? super T> comparator) {
        this.comparator = comparator;
        TreeSet<T> temp = new TreeSet<>(comparator);
        temp.addAll(coll);
        list = List.copyOf(temp);
    }

    public Comparator<? super T> comparator() {
        return comparator;
    }

    public int size() {
        return list.size();
    }

    public boolean contains(Object obj) {
        try {
            return getIndex((T) obj) >= 0;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        if (comparator != null && comparator.compare(fromElement, toElement) > 0 ||
                comparator == null && ((Comparable<T>) fromElement).compareTo(toElement) > 0)
            throw new IllegalArgumentException("Left border greater than right border");
        int l = getPosition(fromElement, true, !fromInclusive);
        int r = getPosition(toElement, false, !toInclusive);
        return l > r ? new ArraySet<>(comparator) : new ArraySet<>(list.subList(l, r + 1), comparator);
    }

    public T last() {
        return getElement(size() - 1);
    }

    public T first() {
        return getElement(0);
    }

    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        return isEmpty() ? this : new ArraySet<>(list.subList(0, getPosition(toElement, false, !inclusive) + 1), comparator);
    }

    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        return isEmpty() ? new ArraySet<>(comparator) : new ArraySet<>(list.subList(getPosition(fromElement, true, !inclusive), size()), comparator);
    }

    public SortedSet<T> subSet(T fromElement, T toElement) {
        if (comparator != null && comparator.compare(fromElement, toElement) > 0 ||
                comparator == null && ((Comparable<T>) fromElement).compareTo(toElement) > 0)
            throw new IllegalArgumentException("Left border greater than right border");
        return subSet(fromElement, true, toElement, false);
    }

    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(new ReversedList<>(list), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<T> descendingIterator() {
        return null;
    }

    public T pollFirst() {
        throw new UnsupportedOperationException("pollLast not supported");
    }

    public T pollLast() {
        throw new UnsupportedOperationException("pollLast not supported");
    }

    public T higher(T element) {
        return getElementNonThrow(getPosition(element, true, true));
    }

    public T ceiling(T element) {
        return getElementNonThrow(getPosition(element, true, false));
    }

    public T lower(T element) {
        return getElementNonThrow(getPosition(element, false, true));
    }

    public T floor(T element) {
        return getElementNonThrow(getPosition(element, false, false));
    }

    public Iterator<T> iterator() {
        return Collections.unmodifiableList(list).iterator();
    }

    public Iterator<T> endingIterator() {
        return descendingSet().iterator();
    }
}
