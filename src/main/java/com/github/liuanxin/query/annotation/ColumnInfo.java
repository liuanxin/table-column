package com.github.liuanxin.query.annotation;

import com.github.liuanxin.query.enums.TableRelationType;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ColumnInfo {

    /** table column name */
    String value();

    /** table column comment */
    String desc() default "";

    /** table column alias, use column name if empty */
    String alias() default "";

    /** true: this field is not associated with a column */
    boolean ignore() default false;

    /** true: this column is primary key */
    boolean primary() default false;

    int varcharLength() default 0;

    /** just set on child table, no need to mark on the main table */
    TableRelationType relationType() default TableRelationType.NULL;

    /** use if relationType has not NULL */
    String relationTable() default "";

    /** use if relationType has not NULL */
    String relationColumn() default "";
}
