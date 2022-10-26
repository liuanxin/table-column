package com.github.liuanxin.query.function;

import java.io.Serializable;
import java.util.function.Function;

/**
 * <pre>
 * public static &lt;T&gt; void func(FunctionSerialize&lt;T, ?&gt; func) {
 * }
 *
 * class Example {
 *     private Long id;
 *
 *     public Long getId() { return id; }
 *     public void setId(Long id) { this.id = id; }
 * }
 *
 * public void main(String[] args) {
 *     Example example = new Example();
 *     // func(example::getId); // compile error : <span style="color:red">Cannot resolve method 'getId'</span>
 *     func(Example::getId);
 * }
 * </pre>
 */
public interface FunctionSerialize<T, R> extends Function<T, R>, Serializable {
}
