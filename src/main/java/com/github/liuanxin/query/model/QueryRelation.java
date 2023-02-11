package com.github.liuanxin.query.model;

import com.github.liuanxin.query.enums.JoinType;

import java.io.Serializable;
import java.util.Objects;

public class QueryRelation implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Class<?> left;
    private final JoinType type;
    private final Class<?> right;

    public QueryRelation(Class<?> left, JoinType type, Class<?> right) {
        this.left = left;
        this.type = type;
        this.right = right;
    }

    public Class<?> getLeft() {
        return left;
    }
    public JoinType getType() {
        return type;
    }
    public Class<?> getRight() {
        return right;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryRelation that = (QueryRelation) o;
        return Objects.equals(left, that.left) && type == that.type && Objects.equals(right, that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, type, right);
    }

    @Override
    public String toString() {
        return "QueryRelation{" +
                "left=" + left +
                ", type=" + type +
                ", right=" + right +
                '}';
    }
}
