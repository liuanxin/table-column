package com.github.liuanxin.query.annotation;

import com.github.liuanxin.query.enums.TableRelationType;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RelationInfo {

    /** just set on child table, no need to mark on the main table */
    TableRelationType type();

    /** use if type has not NULL, use on child Table's column, no support multiple column by master Table */
    String masterTable();

    /** use if type has not NULL, use on child Table's column, no support multiple column by master Table */
    String masterColumn();
}
