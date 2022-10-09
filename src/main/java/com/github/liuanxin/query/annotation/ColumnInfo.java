package com.github.liuanxin.query.annotation;

import com.github.liuanxin.query.enums.SchemaRelationType;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ColumnInfo {

    /** schema column name */
    String value();

    /** schema column comment */
    String desc() default "";

    /** schema column alias, use column name if empty */
    String alias() default "";

    /** true: this field is not associated with a column */
    boolean ignore() default false;

    /** true: this column is primary key */
    boolean primary() default false;

    int varcharLength() default 0;

    /** just set on child schema, no need to mark on the main schema */
    SchemaRelationType relationType() default SchemaRelationType.NULL;

    /** use if relationType has not NULL */
    String relationSchema() default "";

    /** use if relationType has not NULL */
    String relationColumn() default "";
}
