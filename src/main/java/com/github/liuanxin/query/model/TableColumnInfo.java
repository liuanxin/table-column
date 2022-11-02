package com.github.liuanxin.query.model;

import com.github.liuanxin.query.constant.QueryConst;
import com.github.liuanxin.query.util.QueryUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableColumnInfo {

    private final Map<String, String> aliasMap;
    private final Map<String, String> tableClassMap;
    private final Map<String, Table> tableMap;

    private Map<String, Map<String, TableColumnRelation>> childRelationMap;
    private Map<String, Map<String, TableColumnRelation>> masterChildTableMap;

    public TableColumnInfo(Map<String, String> aliasMap, Map<String, String> tableClassMap, Map<String, Table> tableMap) {
        this.aliasMap = aliasMap;
        this.tableClassMap = tableClassMap;
        this.tableMap = tableMap;
    }

    public void handleRelation(List<TableColumnRelation> relationList) {
        Map<String, Map<String, TableColumnRelation>> childRelationMap = new HashMap<>();
        Map<String, Map<String, TableColumnRelation>> masterChildTableMap = new HashMap<>();
        if (QueryUtil.isNotEmpty(relationList)) {
            for (TableColumnRelation relation : relationList) {
                String childTable = relation.getOneOrManyTable();
                String childColumn = relation.getOneOrManyColumn();
                String masterTable = relation.getOneTable();

                childRelationMap.computeIfAbsent(childTable, k -> new HashMap<>()).put(childColumn, relation);
                masterChildTableMap.computeIfAbsent(masterTable, k -> new HashMap<>()).put(childTable, relation);
            }
        }
        this.childRelationMap = childRelationMap;
        this.masterChildTableMap = masterChildTableMap;
    }


    public Collection<Table> allTable() {
        return tableMap.values();
    }

    public Table findTableByClass(Class<?> clazz) {
        String tableName = tableClassMap.get(clazz.getName());
        return QueryUtil.isEmpty(tableName) ? null : tableMap.get(tableName);
    }

    public Table findTable(String tableName) {
        if (QueryUtil.isEmpty(tableName)) {
            return null;
        }
        String tn = tableName.trim();
        String ta = aliasMap.get(QueryConst.TABLE_PREFIX + tn);
        return tableMap.get(QueryUtil.defaultIfBlank(ta, tn));
    }

    public TableColumn findTableColumn(Table table, String columnName) {
        if (QueryUtil.isNull(table) || QueryUtil.isEmpty(columnName)) {
            return null;
        }
        Map<String, TableColumn> columnMap = table.getColumnMap();
        if (QueryUtil.isEmpty(columnMap)) {
            return null;
        }
        String cn = columnName.trim();
        String ca = aliasMap.get(QueryConst.COLUMN_PREFIX + cn);
        return columnMap.get(QueryUtil.defaultIfBlank(ca, cn));
    }

    public TableColumn findTableColumn(String tableName, String columnName) {
        Table table = findTable(tableName);
        return QueryUtil.isNull(table) ? null : findTableColumn(table, columnName);
    }

    public TableColumnRelation findRelationByMasterChild(String masterTable, String childTable) {
        Table table = findTable(masterTable);
        if (QueryUtil.isNull(table)) {
            return null;
        }

        Map<String, TableColumnRelation> relationMap = masterChildTableMap.get(table.getName());
        if (QueryUtil.isEmpty(relationMap)) {
            return null;
        }

        Table child = findTable(childTable);
        return QueryUtil.isNull(child) ? null : relationMap.get(child.getName());
    }

    public TableColumnRelation findRelationByChild(String childTable, String childColumn) {
        Table table = findTable(childTable);
        if (QueryUtil.isNull(table)) {
            return null;
        }

        Map<String, TableColumnRelation> relationMap = childRelationMap.get(table.getName());
        if (QueryUtil.isEmpty(relationMap)) {
            return null;
        }

        TableColumn column = findTableColumn(table, childColumn);
        return QueryUtil.isNull(column) ? null : relationMap.get(column.getName());
    }
}
