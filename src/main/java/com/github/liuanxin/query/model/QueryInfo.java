package com.github.liuanxin.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class QueryInfo {

    /** 表名 */
    private String name;

    /** 表说明 */
    private String desc;

    /** 列信息 */
    private List<QueryColumn> columnList;

    public QueryInfo() {
    }

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

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class QueryColumn {

        /** 列名 */
        private String name;

        /** 列说明 */
        private String desc;

        /** 列类型 */
        private String type;

        /** 列类型是字符串时的长度 */
        private Integer length;

        /** 关联信息 */
        private String relation;

        public QueryColumn() {
        }

        public QueryColumn(String name, String desc, String type, Integer length, String relation) {
            this.name = name;
            this.desc = desc;
            this.type = type;
            this.length = length;
            this.relation = relation;
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

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getLength() {
            return length;
        }

        public void setLength(Integer length) {
            this.length = length;
        }

        public String getRelation() {
            return relation;
        }

        public void setRelation(String relation) {
            this.relation = relation;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof QueryColumn)) return false;
            QueryColumn that = (QueryColumn) o;
            return Objects.equals(name, that.name) && Objects.equals(desc, that.desc)
                    && Objects.equals(type, that.type) && Objects.equals(length, that.length)
                    && Objects.equals(relation, that.relation);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, desc, type, length, relation);
        }

        @Override
        public String toString() {
            return "QueryColumn{" +
                    "name='" + name + '\'' +
                    ", desc='" + desc + '\'' +
                    ", type='" + type + '\'' +
                    ", length=" + length +
                    ", relation='" + relation + '\'' +
                    '}';
        }
    }
}
