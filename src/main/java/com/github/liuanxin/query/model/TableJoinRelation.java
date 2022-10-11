package com.github.liuanxin.query.model;

import com.github.liuanxin.query.enums.JoinType;
import com.github.liuanxin.query.util.QuerySqlUtil;

import java.util.Objects;

public class TableJoinRelation {

    private Table masterTable;
    private JoinType joinType;
    private Table childTable;

    public TableJoinRelation() {}
    public TableJoinRelation(Table masterTable, JoinType joinType, Table childTable) {
        this.masterTable = masterTable;
        this.joinType = joinType;
        this.childTable = childTable;
    }

    public Table getMasterTable() {
        return masterTable;
    }
    public void setMasterTable(Table masterTable) {
        this.masterTable = masterTable;
    }

    public JoinType getJoinType() {
        return joinType;
    }
    public void setJoinType(JoinType joinType) {
        this.joinType = joinType;
    }

    public Table getChildTable() {
        return childTable;
    }
    public void setChildTable(Table childTable) {
        this.childTable = childTable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TableJoinRelation)) return false;
        TableJoinRelation that = (TableJoinRelation) o;
        return Objects.equals(masterTable, that.masterTable) && joinType == that.joinType
                && Objects.equals(childTable, that.childTable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(masterTable, joinType, childTable);
    }

    @Override
    public String toString() {
        return "TableJoinRelation{" +
                "masterTable=" + masterTable +
                ", joinType=" + joinType +
                ", childTable=" + childTable +
                '}';
    }


    public String generateJoin(TableColumnInfo tcInfo) {
        String masterTableName = masterTable.getName();
        String childTableName = childTable.getName();
        TableColumnRelation relation = tcInfo.findRelationByMasterChild(masterTableName, childTableName);
        String masterAlias = masterTable.getAlias();
        String childAlias = QuerySqlUtil.toSqlField(childTable.getAlias());
        return " " + joinType.name() +
                " JOIN " + QuerySqlUtil.toSqlField(childTableName) +
                " AS " + childAlias + " ON " + childAlias +
                "." + QuerySqlUtil.toSqlField(relation.getOneOrManyColumn()) +
                " = " + QuerySqlUtil.toSqlField(masterAlias) +
                "." + QuerySqlUtil.toSqlField(relation.getOneColumn());
    }
}
