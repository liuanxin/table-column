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
    private Boolean writeNeedValue;

    /** 列类型是字符串时的长度 */
    private Integer length;

    /** 关联表 */
    private String relationTable;

    /** 关联字段 */
    private String relationColumn;

    public QueryColumn() {}
    public QueryColumn(String name, String desc, String type, Boolean writeNeedValue,
                       Integer length, String relationTable, String relationColumn) {
        this.name = name;
        this.desc = desc;
        this.type = type;
        this.writeNeedValue = writeNeedValue;
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

    public Boolean isWriteNeedValue() {
        return writeNeedValue;
    }

    public void setWriteNeedValue(Boolean writeNeedValue) {
        this.writeNeedValue = writeNeedValue;
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
        if (o == null || getClass() != o.getClass()) return false;
        QueryColumn that = (QueryColumn) o;
        return Objects.equals(name, that.name) && Objects.equals(desc, that.desc)
                && Objects.equals(type, that.type) && Objects.equals(writeNeedValue, that.writeNeedValue)
                && Objects.equals(length, that.length) && Objects.equals(relationTable, that.relationTable)
                && Objects.equals(relationColumn, that.relationColumn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, desc, type, writeNeedValue, length, relationTable, relationColumn);
    }

    @Override
    public String toString() {
        return "QueryColumn{" +
                "name='" + name + '\'' +
                ", desc='" + desc + '\'' +
                ", type='" + type + '\'' +
                ", writeNeedValue=" + writeNeedValue +
                ", length=" + length +
                ", relationTable='" + relationTable + '\'' +
                ", relationColumn='" + relationColumn + '\'' +
                '}';
    }
}
