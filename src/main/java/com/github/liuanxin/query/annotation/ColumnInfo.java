package com.github.liuanxin.query.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ColumnInfo {

    /** table column name */
    String value() default "";

    /** table column alias, use class - field name if empty */
    String alias() default "";

    /** table column comment */
    String desc() default "";

    /** true: this column is primary key */
    boolean primary() default false;

    /** varchar column's length */
    int strLen() default 0;

    /** true: this column not null */
    boolean notNull() default false;

    /** true: this column has default value */
    boolean hasDefault() default false;
}
