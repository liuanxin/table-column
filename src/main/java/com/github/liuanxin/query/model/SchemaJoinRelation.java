package com.github.liuanxin.query.model;

import com.github.liuanxin.query.enums.JoinType;
import com.github.liuanxin.query.util.QuerySqlUtil;

import java.util.Objects;

public class SchemaJoinRelation {

    private Schema masterSchema;
    private JoinType joinType;
    private Schema childSchema;

    public SchemaJoinRelation() {
    }

    public SchemaJoinRelation(Schema masterSchema, JoinType joinType, Schema childSchema) {
        this.masterSchema = masterSchema;
        this.joinType = joinType;
        this.childSchema = childSchema;
    }

    public Schema getMasterSchema() {
        return masterSchema;
    }

    public void setMasterSchema(Schema masterSchema) {
        this.masterSchema = masterSchema;
    }

    public JoinType getJoinType() {
        return joinType;
    }

    public void setJoinType(JoinType joinType) {
        this.joinType = joinType;
    }

    public Schema getChildSchema() {
        return childSchema;
    }

    public void setChildSchema(Schema childSchema) {
        this.childSchema = childSchema;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SchemaJoinRelation)) return false;
        SchemaJoinRelation that = (SchemaJoinRelation) o;
        return Objects.equals(masterSchema, that.masterSchema) && joinType == that.joinType
                && Objects.equals(childSchema, that.childSchema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(masterSchema, joinType, childSchema);
    }

    @Override
    public String toString() {
        return "SchemaJoinRelation{" +
                "masterSchema=" + masterSchema +
                ", joinType=" + joinType +
                ", childSchema=" + childSchema +
                '}';
    }


    public String generateJoin(SchemaColumnInfo scInfo) {
        String masterSchemaName = masterSchema.getName();
        String childSchemaName = childSchema.getName();
        SchemaColumnRelation relation = scInfo.findRelationByMasterChild(masterSchemaName, childSchemaName);
        String masterAlias = masterSchema.getAlias();
        String childAlias = QuerySqlUtil.toSqlField(childSchema.getAlias());
        return " " + joinType.name() +
                " JOIN " + QuerySqlUtil.toSqlField(childSchemaName) +
                " AS " + childAlias + " ON " + childAlias +
                "." + QuerySqlUtil.toSqlField(relation.getOneOrManyColumn()) +
                " = " + QuerySqlUtil.toSqlField(masterAlias) +
                "." + QuerySqlUtil.toSqlField(relation.getOneColumn());
    }
}
