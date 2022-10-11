package com.github.liuanxin.query.util;

import java.io.Serializable;
import java.util.function.Supplier;

public interface SupplierSerialize<T> extends Supplier<T>, Serializable {
}
