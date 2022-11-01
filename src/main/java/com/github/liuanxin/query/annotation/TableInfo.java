package com.github.liuanxin.query.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TableInfo {

    /** table name */
    String value() default "";

    /** table alias, use class name if empty */
    String alias() default "";

    /** table comment */
    String desc() default "";
}
