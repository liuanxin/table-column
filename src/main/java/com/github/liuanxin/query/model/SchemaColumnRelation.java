package com.github.liuanxin.query.model;

import com.github.liuanxin.query.enums.SchemaRelationType;

import java.util.Objects;

public class SchemaColumnRelation {

    private String oneSchema;
    private String oneColumn;
    private SchemaRelationType type;
    private String oneOrManySchema;
    private String oneOrManyColumn;

    public SchemaColumnRelation(String oneSchema, String oneColumn, SchemaRelationType type, String oneOrManySchema, String oneOrManyColumn) {
        this.oneSchema = oneSchema;
        this.oneColumn = oneColumn;
        this.type = type;
        this.oneOrManySchema = oneOrManySchema;
        this.oneOrManyColumn = oneOrManyColumn;
    }

    public String getOneSchema() {
        return oneSchema;
    }

    public void setOneSchema(String oneSchema) {
        this.oneSchema = oneSchema;
    }

    public String getOneColumn() {
        return oneColumn;
    }

    public void setOneColumn(String oneColumn) {
        this.oneColumn = oneColumn;
    }

    public SchemaRelationType getType() {
        return type;
    }

    public void setType(SchemaRelationType type) {
        this.type = type;
    }

    public String getOneOrManySchema() {
        return oneOrManySchema;
    }

    public void setOneOrManySchema(String oneOrManySchema) {
        this.oneOrManySchema = oneOrManySchema;
    }

    public String getOneOrManyColumn() {
        return oneOrManyColumn;
    }

    public void setOneOrManyColumn(String oneOrManyColumn) {
        this.oneOrManyColumn = oneOrManyColumn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        SchemaColumnRelation that = (SchemaColumnRelation) o;
        return oneSchema.equals(that.oneSchema) && oneColumn.equals(that.oneColumn)
                && oneOrManySchema.equals(that.oneOrManySchema) && oneOrManyColumn.equals(that.oneOrManyColumn);
    }
    @Override
    public int hashCode() {
        return Objects.hash(oneSchema, oneColumn, oneOrManySchema, oneOrManyColumn);
    }

    @Override
    public String toString() {
        return "SchemaColumnRelation{" +
                "oneSchema='" + oneSchema + '\'' +
                ", oneColumn='" + oneColumn + '\'' +
                ", type=" + type +
                ", oneOrManySchema='" + oneOrManySchema + '\'' +
                ", oneOrManyColumn='" + oneOrManyColumn + '\'' +
                '}';
    }
}
