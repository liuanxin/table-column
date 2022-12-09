package com.github.liuanxin.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class QueryInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 表名 */
    private String name;

    /** 表说明 */
    private String desc;

    /** 列信息 */
    private List<QueryColumn> columnList;

    public QueryInfo() {}
    public QueryInfo(String name, String desc, List<QueryColumn> columnList) {
        this.name = name;
        this.desc = desc;
        this.columnList = columnList;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }
    public void setDesc(String desc) {
        this.desc = desc;
    }

    public List<QueryColumn> getColumnList() {
        return columnList;
    }
    public void setColumnList(List<QueryColumn> columnList) {
        this.columnList = columnList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QueryInfo)) return false;
        QueryInfo queryInfo = (QueryInfo) o;
        return Objects.equals(name, queryInfo.name) && Objects.equals(desc, queryInfo.desc)
                && Objects.equals(columnList, queryInfo.columnList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, desc, columnList);
    }

    @Override
    public String toString() {
        return "QueryInfo{" +
                "name='" + name + '\'' +
                ", desc='" + desc + '\'' +
                ", columnList=" + columnList +
                '}';
    }

}
