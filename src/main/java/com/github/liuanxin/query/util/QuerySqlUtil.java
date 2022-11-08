package com.github.liuanxin.query.util;

import com.github.liuanxin.query.constant.QueryConst;
import com.github.liuanxin.query.model.*;

import java.util.*;

public class QuerySqlUtil {

    public static String toSqlField(String field) {
        return MysqlKeyWordUtil.hasKeyWord(field) ? ("`" + field + "`") : field;
    }

    public static Object toValue(Class<?> type, Object value) {
        if (QueryConst.BOOLEAN_TYPE_SET.contains(type)) {
            return QueryUtil.toBoolean(value);
        } else if (QueryConst.INT_TYPE_SET.contains(type)) {
            return QueryUtil.toInteger(value);
        } else if (QueryConst.LONG_TYPE_SET.contains(type)) {
            return QueryUtil.toLonger(value);
        } else if (Number.class.isAssignableFrom(type)) {
            return QueryUtil.toDecimal(value);
        } else if (Date.class.isAssignableFrom(type)) {
            return QueryUtil.toDate(value);
        } else {
            return value;
        }
    }
    public static String toPrintValue(Class<?> type, Object value) {
        if (QueryConst.BOOLEAN_TYPE_SET.contains(type)) {
            return QueryUtil.toBool(value) ? "1" : "0";
        } else if (QueryConst.INT_TYPE_SET.contains(type)) {
            return QueryUtil.toStr(QueryUtil.toInteger(value));
        } else if (QueryConst.LONG_TYPE_SET.contains(type)) {
            return QueryUtil.toStr(QueryUtil.toLonger(value));
        } else if (Number.class.isAssignableFrom(type)) {
            return QueryUtil.toStr(QueryUtil.toDecimal(value));
        } else if (Date.class.isAssignableFrom(type)) {
            return "'" + QueryUtil.formatDate(QueryUtil.toDate(value)) + "'";
        } else {
            return "'" + value + "'";
        }
    }

    public static String toFromSql(TableColumnInfo tcInfo, String mainTable, Set<TableJoinRelation> relationSet) {
        StringBuilder sbd = new StringBuilder(" FROM ");
        Table table = tcInfo.findTable(mainTable);
        String mainTableName = table.getName();
        sbd.append(toSqlField(mainTableName));
        if (QueryUtil.isNotEmpty(relationSet)) {
            sbd.append(" AS ").append(toSqlField(table.getAlias()));
            for (TableJoinRelation joinRelation : relationSet) {
                sbd.append(joinRelation.generateJoin(tcInfo));
            }
        }
        return sbd.toString();
    }

    public static String toCountGroupSql(String selectSql, String selectPrint, StringBuilder printSql) {
        printSql.append("SELECT COUNT(*) FROM ( ").append(selectPrint).append(" ) TMP");
        return "SELECT COUNT(*) FROM ( " + selectSql + " ) TMP";
    }

    public static String toSelectGroupSql(TableColumnInfo tcInfo, String fromAndWhere, String fromAndWherePrint,
                                          String mainTable, ReqResult result, boolean needAlias,
                                          List<Object> params, StringBuilder printSql) {
        String selectField = result.generateAllSelectSql(mainTable, tcInfo, needAlias);
        boolean emptySelect = QueryUtil.isEmpty(selectField);

        // SELECT ... FROM ... WHERE ... GROUP BY ... HAVING ...
        StringBuilder sbd = new StringBuilder();
        sbd.append("SELECT ");
        printSql.append("SELECT ");
        if (!emptySelect) {
            sbd.append(selectField);
            printSql.append(selectField);
        }

        String functionSql = result.generateFunctionSql(mainTable, needAlias, tcInfo);
        if (QueryUtil.isNotEmpty(functionSql)) {
            if (!emptySelect) {
                sbd.append(", ");
                printSql.append(", ");
            }
            sbd.append(functionSql);
            printSql.append(functionSql);
        }

        sbd.append(fromAndWhere);
        printSql.append(fromAndWherePrint);

        String group = result.generateGroupSql(mainTable, needAlias, tcInfo);
        sbd.append(group);
        printSql.append(group);

        StringBuilder havingPrint = new StringBuilder();
        String having = result.generateHavingSql(mainTable, needAlias, tcInfo, params, havingPrint);
        sbd.append(having);
        printSql.append(havingPrint);
        return sbd.toString();
    }

    public static String toCountWithoutGroupSql(TableColumnInfo tcInfo, String fromAndWhere,
                                                String fromAndWherePrint, String mainTable, boolean needAlias,
                                                boolean queryHasMany, StringBuilder printSql) {
        if (queryHasMany) {
            // SELECT COUNT(DISTINCT xx.id) FROM ...
            String select = tcInfo.findTable(mainTable).idSelect(needAlias);
            printSql.append(String.format("SELECT COUNT(DISTINCT %s)%s", select, fromAndWherePrint));
            return String.format("SELECT COUNT(DISTINCT %s)%s", select, fromAndWhere);
        } else {
            printSql.append("SELECT COUNT(*)").append(fromAndWherePrint);
            return "SELECT COUNT(*)" + fromAndWhere;
        }
    }

    public static String toPageSql(TableColumnInfo tcInfo, String fromAndWhere, String fromAndWherePrint,
                                   String mainTable, ReqParam param, ReqResult result, boolean needAlias,
                                   List<Object> params, StringBuilder printSql) {
        String selectColumn = result.generateAllSelectSql(mainTable, tcInfo, needAlias);
        // SELECT ... FROM ... WHERE ... ORDER BY ... limit ...
        return toAppendSql(tcInfo, fromAndWhere, fromAndWherePrint, mainTable, param, needAlias, selectColumn, params, printSql);
    }

    private static String toAppendSql(TableColumnInfo tcInfo, String fromAndWhere, String fromAndWherePrint,
                                      String mainTable, ReqParam param, boolean needAlias, String selectColumn,
                                      List<Object> params, StringBuilder printSql) {
        String orderSql = param.generateOrderSql(mainTable, needAlias, tcInfo);
        StringBuilder pagePrint = new StringBuilder();
        String pageSql = param.generatePageSql(params, pagePrint);
        printSql.append("SELECT ").append(selectColumn).append(fromAndWherePrint).append(orderSql).append(pagePrint);
        return "SELECT " + selectColumn + fromAndWhere + orderSql + pageSql;
    }

    public static String toIdPageSql(TableColumnInfo tcInfo, String fromAndWhere, String fromAndWherePrint, String mainTable,
                                     boolean needAlias, ReqParam param, List<Object> params, StringBuilder printSql) {
        String idSelect = tcInfo.findTable(mainTable).idSelect(needAlias);
        // SELECT id FROM ... WHERE ... ORDER BY ... LIMIT ...
        return toAppendSql(tcInfo, fromAndWhere, fromAndWherePrint, mainTable, param, needAlias, idSelect, params, printSql);
    }
    public static String toSelectWithIdSql(TableColumnInfo tcInfo, String mainTable, String tables,
                                           ReqResult result, List<Map<String, Object>> idList,
                                           boolean needAlias, List<Object> params, StringBuilder printSql) {
        // SELECT ... FROM ... WHERE id IN (x, y, z)
        String selectColumn = result.generateAllSelectSql(mainTable, tcInfo, needAlias);
        Table table = tcInfo.findTable(mainTable);
        String idColumn = table.idWhere(needAlias);
        List<String> idKey = table.getIdKey();
        StringJoiner sj = new StringJoiner(", ");
        StringJoiner print = new StringJoiner(", ");
        for (Map<String, Object> idMap : idList) {
            if (idKey.size() > 1) {
                // WHERE ( id1, id2 ) IN ( (X, XX), (Y, YY) )
                StringJoiner innerJoiner = new StringJoiner(", ");
                StringJoiner innerPrint = new StringJoiner(", ");
                for (String id : idKey) {
                    Object data = idMap.get(id);
                    innerJoiner.add("?");
                    innerPrint.add(QuerySqlUtil.toPrintValue(data.getClass(), data));
                    params.add(data);
                }
                sj.add("(" + innerJoiner + ")");
                print.add("(" + innerPrint + ")");
            } else {
                // WHERE id IN (x, y, z)
                Object data = idMap.get(idKey.get(0));
                sj.add("?");
                print.add(QuerySqlUtil.toPrintValue(data.getClass(), data));
                params.add(data);
            }
        }
        printSql.append("SELECT ").append(selectColumn).append(" FROM ").append(tables)
                .append(" WHERE ").append(idColumn).append(" IN (").append(print).append(")");
        return "SELECT " + selectColumn + " FROM " + tables + " WHERE " + idColumn + " IN (" + sj + ")";
    }

    public static String toInnerSql(String selectColumn, String table, String relationColumn,
                                    List<Object> relationIds, List<Object> params, StringBuilder printSql) {
        StringJoiner in = new StringJoiner(", ");
        StringJoiner print = new StringJoiner(", ");
        for (Object relationId : relationIds) {
            in.add("?");
            print.add(QuerySqlUtil.toPrintValue(relationId.getClass(), relationId));
            params.add(relationId);
        }
        printSql.append("SELECT ").append(selectColumn).append(" FROM ").append(table)
                .append(" WHERE ").append(relationColumn).append(" IN (").append(print).append(")");
        return "SELECT " + selectColumn + " FROM " + table + " WHERE " + relationColumn + " IN" + " (" + in + ")";
    }
}
