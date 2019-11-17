package ru.ifmo.rain.kononov.arrayset;

import java.util.AbstractList;
import java.util.List;

public class ReversedList<T> extends AbstractList<T> {
    private final List<T> content;

    ReversedList(List<T> list) {
        content = list;
    }

    public T get(int ind) {
        return content.get(content.size() - ind - 1);
    }

    public int size() {
        return content.size();
    }
}