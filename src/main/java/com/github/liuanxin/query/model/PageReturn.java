package com.github.liuanxin.query.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@SuppressWarnings({"rawtypes", "unchecked"})
public class PageReturn<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final PageReturn EMPTY = new PageReturn<>(0L, Collections.emptyList());

    private long count;
    private List<T> list;

    public PageReturn() {}
    public PageReturn(long count, List<T> list) {
        this.count = count;
        this.list = list;
    }

    public long getCount() {
        return count;
    }
    public void setCount(long count) {
        this.count = count;
    }

    public List<T> getList() {
        return list;
    }
    public void setList(List<T> list) {
        this.list = list;
    }


    public static <T> PageReturn<T> page(long count, int index, int limit, Supplier<List<T>> supplier) {
        if (count == 0) {
            return EMPTY;
        }
        if ((index == 1) || ((long) index * limit <= count)) {
            return new PageReturn<>(count, supplier.get());
        } else {
            return new PageReturn<>(count, Collections.emptyList());
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageReturn<?> that = (PageReturn<?>) o;
        return count == that.count && Objects.equals(list, that.list);
    }

    @Override
    public int hashCode() {
        return Objects.hash(count, list);
    }

    @Override
    public String toString() {
        return "PageReturn{" +
                "count=" + count +
                ", list=" + list +
                '}';
    }
}
