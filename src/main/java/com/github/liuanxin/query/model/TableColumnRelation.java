package com.github.liuanxin.query.model;

import com.github.liuanxin.query.enums.TableRelationType;

import java.io.Serializable;
import java.util.Objects;

public class TableColumnRelation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String oneTable;
    private String oneColumn;
    private TableRelationType type;
    private String oneOrManyTable;
    private String oneOrManyColumn;

    public TableColumnRelation() {}
    public TableColumnRelation(String oneTable, String oneColumn, TableRelationType type,
                               String oneOrManyTable, String oneOrManyColumn) {
        this.oneTable = oneTable;
        this.oneColumn = oneColumn;
        this.type = type;
        this.oneOrManyTable = oneOrManyTable;
        this.oneOrManyColumn = oneOrManyColumn;
    }

    public String getOneTable() {
        return oneTable;
    }
    public void setOneTable(String oneTable) {
        this.oneTable = oneTable;
    }

    public String getOneColumn() {
        return oneColumn;
    }
    public void setOneColumn(String oneColumn) {
        this.oneColumn = oneColumn;
    }

    public TableRelationType getType() {
        return type;
    }
    public void setType(TableRelationType type) {
        this.type = type;
    }

    public String getOneOrManyTable() {
        return oneOrManyTable;
    }
    public void setOneOrManyTable(String oneOrManyTable) {
        this.oneOrManyTable = oneOrManyTable;
    }

    public String getOneOrManyColumn() {
        return oneOrManyColumn;
    }
    public void setOneOrManyColumn(String oneOrManyColumn) {
        this.oneOrManyColumn = oneOrManyColumn;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableColumnRelation that = (TableColumnRelation) o;
        return Objects.equals(oneTable, that.oneTable) && Objects.equals(oneColumn, that.oneColumn)
                && type == that.type && Objects.equals(oneOrManyTable, that.oneOrManyTable)
                && Objects.equals(oneOrManyColumn, that.oneOrManyColumn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oneTable, oneColumn, type, oneOrManyTable, oneOrManyColumn);
    }

    @Override
    public String toString() {
        return "TableColumnRelation{" +
                "oneTable='" + oneTable + '\'' +
                ", oneColumn='" + oneColumn + '\'' +
                ", type=" + type +
                ", oneOrManyTable='" + oneOrManyTable + '\'' +
                ", oneOrManyColumn='" + oneOrManyColumn + '\'' +
                '}';
    }
}
