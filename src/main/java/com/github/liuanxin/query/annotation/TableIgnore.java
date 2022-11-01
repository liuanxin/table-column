package com.github.liuanxin.query.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TableIgnore {

    /** true: this class is not associated with a table */
    boolean value() default true;
}
