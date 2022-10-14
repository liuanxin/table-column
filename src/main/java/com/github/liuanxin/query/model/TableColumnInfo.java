package com.github.liuanxin.query.model;

import com.github.liuanxin.query.constant.QueryConst;
import com.github.liuanxin.query.util.QueryUtil;

import java.util.*;

public class TableColumnInfo {

    private final Map<String, String> aliasMap;
    private final Map<String, String> tableClassMap;
    private final Map<String, Table> tableMap;

    private final Map<String, Map<String, TableColumnRelation>> childRelationMap;
    private final Map<String, Map<String, TableColumnRelation>> masterChildTableMap;

    public TableColumnInfo(Map<String, String> aliasMap, Map<String, String> tableClassMap,
                           Map<String, Table> tableMap, List<TableColumnRelation> relationList) {
        this.aliasMap = aliasMap;
        this.tableClassMap = tableClassMap;
        this.tableMap = tableMap;

        Map<String, Map<String, TableColumnRelation>> childRelationMap = new HashMap<>();
        Map<String, Map<String, TableColumnRelation>> masterChildTableMap = new HashMap<>();
        if (QueryUtil.isNotEmpty(relationList)) {
            for (TableColumnRelation relation : relationList) {
                String masterTable = relation.getOneTable();
                String childTable = relation.getOneOrManyTable();
                String childColumn = relation.getOneOrManyColumn();

                Map<String, TableColumnRelation> childRelation = childRelationMap.getOrDefault(childTable, new HashMap<>());
                childRelation.put(childColumn, relation);
                childRelationMap.put(childTable, childRelation);

                Map<String, TableColumnRelation> masterChildRelation = masterChildTableMap.getOrDefault(masterTable, new HashMap<>());
                masterChildRelation.put(childTable, relation);
                masterChildTableMap.put(masterTable, masterChildRelation);
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
        String tableAlias = aliasMap.get(QueryConst.TABLE_PREFIX + tableName);
        Table table = tableMap.get(tableAlias);
        return QueryUtil.isNull(table) ? tableMap.get(tableName) : table;
    }

    public TableColumn findTableColumn(Table table, String columnName) {
        Map<String, TableColumn> columnMap = table.getColumnMap();
        String columnAlias = aliasMap.get(QueryConst.COLUMN_PREFIX + columnName);
        TableColumn tableColumn = columnMap.get(columnAlias);
        return QueryUtil.isNull(tableColumn) ? columnMap.get(columnName) : tableColumn;
    }

    public TableColumn findTableColumn(String tableName, String columnName) {
        Table table = findTable(tableName);
        return QueryUtil.isNull(table) ? null : findTableColumn(table, columnName);
    }

    public Map<String, TableColumnRelation> findRelationByMaster(String masterTable) {
        if (QueryUtil.isEmpty(masterTable)) {
            return Collections.emptyMap();
        }

        String tableAlias = aliasMap.get(QueryConst.TABLE_PREFIX + masterTable);
        Map<String, TableColumnRelation> relationMap = masterChildTableMap.get(tableAlias);
        return QueryUtil.isEmpty(relationMap) ? masterChildTableMap.get(masterTable) : relationMap;
    }

    public TableColumnRelation findRelationByMasterChild(String masterTable, String childTable) {
        return findRelation(masterChildTableMap, masterTable, childTable);
    }

    private TableColumnRelation findRelation(Map<String, Map<String, TableColumnRelation>> tableRelationMap,
                                             String table, String childTableOrColumn) {
        if (QueryUtil.isEmpty(tableRelationMap) || QueryUtil.isEmpty(table) || QueryUtil.isEmpty(childTableOrColumn)) {
            return null;
        }

        String tableAlias = aliasMap.get(QueryConst.TABLE_PREFIX + table);
        Map<String, TableColumnRelation> relationMap = tableRelationMap.get(tableAlias);
        Map<String, TableColumnRelation> useRelationMap = QueryUtil.isEmpty(relationMap) ? tableRelationMap.get(table) : relationMap;
        if (QueryUtil.isEmpty(useRelationMap)) {
            return null;
        }

        String columnAlias = aliasMap.get(QueryConst.COLUMN_PREFIX + childTableOrColumn);
        TableColumnRelation relation = useRelationMap.get(columnAlias);
        return QueryUtil.isNull(relation) ? useRelationMap.get(childTableOrColumn) : relation;
    }

    public TableColumnRelation findRelationByChild(String childTable, String childColumn) {
        return findRelation(childRelationMap, childTable, childColumn);
    }
}
