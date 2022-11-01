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

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class QueryColumn {

        /** 列名 */
        private String name;

        /** 列说明 */
        private String desc;

        /** 列类型 */
        private String type;

        /** 新增时需要值 */
        private boolean needValue;

        /** 列类型是字符串时的长度 */
        private Integer length;

        /** 关联表 */
        private String relationTable;

        /** 关联字段 */
        private String relationColumn;

        public QueryColumn() {}
        public QueryColumn(String name, String desc, String type, boolean needValue,
                           Integer length, String relationTable, String relationColumn) {
            this.name = name;
            this.desc = desc;
            this.type = type;
            this.needValue = needValue;
            this.length = length;
            this.relationTable = relationTable;
            this.relationColumn = relationColumn;
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

        public boolean isNeedValue() {
            return needValue;
        }
        public void setNeedValue(boolean needValue) {
            this.needValue = needValue;
        }

        public Integer getLength() {
            return length;
        }
        public void setLength(Integer length) {
            this.length = length;
        }

        public String getRelationTable() {
            return relationTable;
        }
        public void setRelationTable(String relationTable) {
            this.relationTable = relationTable;
        }

        public String getRelationColumn() {
            return relationColumn;
        }
        public void setRelationColumn(String relationColumn) {
            this.relationColumn = relationColumn;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof QueryColumn)) return false;
            QueryColumn that = (QueryColumn) o;
            return Objects.equals(name, that.name) && Objects.equals(desc, that.desc)
                    && Objects.equals(type, that.type) && Objects.equals(needValue, that.needValue)
                    && Objects.equals(length, that.length) && Objects.equals(relationTable, that.relationTable)
                    && Objects.equals(relationColumn, that.relationColumn);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, desc, type, needValue, length, relationTable, relationColumn);
        }

        @Override
        public String toString() {
            return "QueryColumn{" +
                    "name='" + name + '\'' +
                    ", desc='" + desc + '\'' +
                    ", type='" + type + '\'' +
                    ", needValue='" + needValue + '\'' +
                    ", length=" + length +
                    ", relationTable='" + relationTable + '\'' +
                    ", relationColumn='" + relationColumn + '\'' +
                    '}';
        }
    }
}
