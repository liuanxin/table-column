package com.github.liuanxin.query.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ColumnIgnore {

    /** true: this field is not associated with a column */
    boolean value() default true;
}
