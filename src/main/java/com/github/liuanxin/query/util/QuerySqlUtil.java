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

    public static String toFromSql(TableColumnInfo tcInfo, String mainTable, List<TableJoinRelation> relationList) {
        StringBuilder sbd = new StringBuilder("FROM ");
        Table table = tcInfo.findTable(mainTable);
        String mainTableName = table.getName();
        sbd.append(toSqlField(mainTableName));
        if (QueryUtil.isNotEmpty(relationList)) {
            sbd.append(" AS ").append(toSqlField(table.getAlias()));
            for (TableJoinRelation joinRelation : relationList) {
                sbd.append(joinRelation.generateJoin(tcInfo));
            }
        }
        return sbd.toString();
    }

    public static String toWhereSql(TableColumnInfo tcInfo, String mainTable, boolean needAlias,
                                    ReqParam param, List<Object> params, StringBuilder printSql) {
        return param.generateWhereSql(mainTable, tcInfo, needAlias, params, printSql);
    }

    public static String toCountGroupSql(String selectSql) {
        return "SELECT COUNT(*) FROM ( " + selectSql + " ) TMP";
    }

    public static String toSelectGroupSql(TableColumnInfo tcInfo, String fromAndWhere, String fromAndWherePrint,
                                          String mainTable, ReqResult result, Set<String> tableSet,
                                          List<Object> params, StringBuilder printSql) {
        boolean needAlias = QueryUtil.isNotEmpty(tableSet);
        String selectField = result.generateAllSelectSql(mainTable, tcInfo, tableSet);
        boolean emptySelect = QueryUtil.isEmpty(selectField);

        // SELECT ... FROM ... WHERE ... GROUP BY ... HAVING ...
        StringBuilder sbd = new StringBuilder("SELECT ");
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

        sbd.append(" ").append(fromAndWhere);
        printSql.append(" ").append(fromAndWherePrint);

        String group = result.generateGroupSql(mainTable, needAlias, tcInfo);
        sbd.append(group);
        printSql.append(group);

        StringBuilder havingPrint = new StringBuilder();
        String having = result.generateHavingSql(mainTable, needAlias, tcInfo, params, havingPrint);
        sbd.append(having);
        printSql.append(havingPrint);
        return sbd.toString();
    }

    public static String toCountWithoutGroupSql(TableColumnInfo tcInfo, String mainTable, boolean needAlias,
                                                boolean queryHasMany, String fromAndWhere, String fromAndWherePrint,
                                                StringBuilder printSql) {
        if (queryHasMany) {
            // SELECT COUNT(DISTINCT xx.id) FROM ...
            printSql.append(String.format("SELECT COUNT(DISTINCT %s) %s", tcInfo.findTable(mainTable).idSelect(needAlias), fromAndWherePrint));
            return String.format("SELECT COUNT(DISTINCT %s) %s", tcInfo.findTable(mainTable).idSelect(needAlias), fromAndWhere);
        } else {
            printSql.append("SELECT COUNT(*) ").append(fromAndWherePrint);
            return "SELECT COUNT(*) " + fromAndWhere;
        }
    }

    public static String toPageWithoutGroupSql(TableColumnInfo tcInfo, String fromAndWhere, String fromAndWherePrint,
                                               String mainTable, ReqParam param, ReqResult result, Set<String> allTableSet,
                                               List<Object> params, StringBuilder printSql) {
        String selectField = result.generateAllSelectSql(mainTable, tcInfo, allTableSet);
        // SELECT ... FROM ... WHERE ... ORDER BY ... limit ...
        String orderSql = param.generateOrderSql(mainTable, QueryUtil.isNotEmpty(allTableSet), tcInfo);
        StringBuilder pagePrint = new StringBuilder();
        String pageSql = param.generatePageSql(params, pagePrint);
        printSql.append("SELECT ").append(selectField).append(" ").append(fromAndWherePrint).append(orderSql).append(pagePrint);
        return "SELECT " + selectField + " " + fromAndWhere + orderSql + pageSql;
    }

    public static String toIdPageSql(TableColumnInfo tcInfo, String fromAndWhere, String fromAndWherePrint, String mainTable,
                                     boolean needAlias, ReqParam param, List<Object> params, StringBuilder printSql) {
        String idSelect = tcInfo.findTable(mainTable).idSelect(needAlias);
        // SELECT id FROM ... WHERE ... ORDER BY ... LIMIT ...
        String orderSql = param.generateOrderSql(mainTable, needAlias, tcInfo);
        StringBuilder pagePrint = new StringBuilder();
        String pageSql = param.generatePageSql(params, pagePrint);
        printSql.append("SELECT ").append(idSelect).append(fromAndWherePrint).append(orderSql).append(pagePrint);
        return "SELECT " + idSelect + fromAndWhere + orderSql + pageSql;
    }
    public static String toSelectWithIdSql(TableColumnInfo tcInfo, String mainTable, String tables,
                                           ReqResult result, List<Map<String, Object>> idList,
                                           Set<String> allTableSet, List<Object> params, StringBuilder printSql) {
        // SELECT ... FROM ... WHERE id IN (x, y, z)
        String selectColumn = result.generateAllSelectSql(mainTable, tcInfo, allTableSet);

        Table table = tcInfo.findTable(mainTable);
        String idColumn = table.idWhere(QueryUtil.isNotEmpty(allTableSet));
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
