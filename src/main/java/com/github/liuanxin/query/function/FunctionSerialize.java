package com.github.liuanxin.query.function;

import java.io.Serializable;
import java.util.function.Function;

/**
 * <pre>
 * public static &lt;T&gt; void func(FunctionSerialize&lt;T, ?&gt; f) {
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
 *     // Example example = new Example();
 *     // func(example::getId); <span style="color:red">// compile error : Cannot resolve method 'getId'</span>
 *     func(Example::getId);
 * }
 * </pre>
 */
public interface FunctionSerialize<T, R> extends Function<T, R>, Serializable {}
