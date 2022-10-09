package com.github.liuanxin.query.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SchemaInfo {

    /** schema name */
    String value();

    /** schema comment */
    String desc() default "";

    /** schema alias, use schema name if empty */
    String alias() default "";

    /** true: this class is not associated with a schema */
    boolean ignore() default false;
}
