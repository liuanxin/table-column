package com.github.liuanxin.query.model;

import java.io.Serializable;
import java.util.function.Function;

@FunctionalInterface
public interface FunctionSerialize<T, R> extends Function<T, R>, Serializable {
}
