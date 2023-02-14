package com.github.liuanxin.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class QueryColumn implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 列名 */
    private String name;

    /** 列说明 */
    private String desc;

    /** 列类型 */
    private String type;

    /** 写入时需要有值 */
    private Boolean writeRequired;

    /** 列类型是字符串时的长度 */
    private Integer maxLength;

    /** 关联表 */
    private String relationTable;

    /** 关联字段 */
    private String relationColumn;

    public QueryColumn() {}
    public QueryColumn(String name, String desc, String type, Boolean writeRequired,
                       Integer maxLength, String relationTable, String relationColumn) {
        this.name = name;
        this.desc = desc;
        this.type = type;
        this.writeRequired = writeRequired;
        this.maxLength = maxLength;
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

    public Boolean isWriteRequired() {
        return writeRequired;
    }

    public void setWriteRequired(Boolean writeRequired) {
        this.writeRequired = writeRequired;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
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
        if (o == null || getClass() != o.getClass()) return false;
        QueryColumn that = (QueryColumn) o;
        return Objects.equals(name, that.name) && Objects.equals(desc, that.desc)
                && Objects.equals(type, that.type) && Objects.equals(writeRequired, that.writeRequired)
                && Objects.equals(maxLength, that.maxLength) && Objects.equals(relationTable, that.relationTable)
                && Objects.equals(relationColumn, that.relationColumn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, desc, type, writeRequired, maxLength, relationTable, relationColumn);
    }

    @Override
    public String toString() {
        return "QueryColumn{" +
                "name='" + name + '\'' +
                ", desc='" + desc + '\'' +
                ", type='" + type + '\'' +
                ", writeRequired=" + writeRequired +
                ", maxLength=" + maxLength +
                ", relationTable='" + relationTable + '\'' +
                ", relationColumn='" + relationColumn + '\'' +
                '}';
    }
}
