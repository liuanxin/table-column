package com.github.liuanxin.query.util;

import com.github.liuanxin.query.model.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public class QuerySqlUtil {

    public static String toSqlField(String field) {
        return MysqlKeyWordUtil.hasKeyWord(field) ? ("`" + field + "`") : field;
    }

    public static String toFromSql(TableColumnInfo tcInfo, String mainTable, List<TableJoinRelation> relationList) {
        StringBuilder sbd = new StringBuilder(" FROM ");
        Table table = tcInfo.findTable(mainTable);
        String mainTableName = table.getName();
        sbd.append(toSqlField(mainTableName));
        if (!relationList.isEmpty()) {
            sbd.append(" AS ").append(table.getAlias());
            for (TableJoinRelation joinRelation : relationList) {
                sbd.append(joinRelation.generateJoin(tcInfo));
            }
        }
        return sbd.toString();
    }

    public static String toWhereSql(TableColumnInfo tcInfo, String mainTable,
                                    boolean needAlias, ReqParam param, List<Object> params) {
        return param.generateWhereSql(mainTable, tcInfo, needAlias, params);
    }

    public static String toCountGroupSql(String selectSql) {
        return "SELECT COUNT(*) FROM ( " + selectSql + " ) TMP";
    }

    public static String toSelectGroupSql(TableColumnInfo tcInfo, String fromAndWhere, String mainTable,
                                          ReqResult result, Set<String> tableSet, List<Object> params) {
        boolean needAlias = !tableSet.isEmpty();
        String selectField = result.generateAllSelectSql(mainTable, tcInfo, tableSet);
        boolean emptySelect = selectField.isEmpty();

        // SELECT ... FROM ... WHERE ... GROUP BY ... HAVING ...
        StringBuilder sbd = new StringBuilder("SELECT ");
        if (!emptySelect) {
            sbd.append(selectField);
        }
        String functionSql = result.generateFunctionSql(mainTable, needAlias, tcInfo);
        if (!functionSql.isEmpty()) {
            if (!emptySelect) {
                sbd.append(", ");
            }
            sbd.append(functionSql);
        }
        sbd.append(fromAndWhere);
        sbd.append(result.generateGroupSql(mainTable, needAlias, tcInfo));
        sbd.append(result.generateHavingSql(mainTable, needAlias, tcInfo, params));
        return sbd.toString();
    }

    public static String toCountWithoutGroupSql(TableColumnInfo tcInfo, String mainTable, boolean needAlias,
                                                boolean queryHasMany, String fromAndWhere) {
        if (queryHasMany) {
            // SELECT COUNT(DISTINCT xx.id) FROM ...
            String idSelect = tcInfo.findTable(mainTable).idSelect(needAlias);
            return String.format("SELECT COUNT(DISTINCT %s) %s", idSelect, fromAndWhere);
        } else {
            return "SELECT COUNT(*) " + fromAndWhere;
        }
    }

    public static String toPageWithoutGroupSql(TableColumnInfo tcInfo, String fromAndWhere, String mainTable,
                                               ReqParam param, ReqResult result,
                                               Set<String> allTableSet, List<Object> params) {
        String selectField = result.generateAllSelectSql(mainTable, tcInfo, allTableSet);
        // SELECT ... FROM ... WHERE ... ORDER BY ... limit ...
        String orderSql = param.generateOrderSql(mainTable, !allTableSet.isEmpty(), tcInfo);
        return "SELECT " + selectField + fromAndWhere + orderSql + param.generatePageSql(params);
    }

    public static String toIdPageSql(TableColumnInfo tcInfo, String fromAndWhere, String mainTable,
                                     boolean needAlias, ReqParam param, List<Object> params) {
        String idSelect = tcInfo.findTable(mainTable).idSelect(needAlias);
        // SELECT id FROM ... WHERE ... ORDER BY ... LIMIT ...
        String orderSql = param.generateOrderSql(mainTable, needAlias, tcInfo);
        return "SELECT " + idSelect + fromAndWhere + orderSql + param.generatePageSql(params);
    }
    public static String toSelectWithIdSql(TableColumnInfo tcInfo, String mainTable, String from,
                                           ReqResult result, List<Map<String, Object>> idList,
                                           Set<String> allTableSet, List<Object> params) {
        // SELECT ... FROM ... WHERE id IN (x, y, z)
        String selectColumn = result.generateAllSelectSql(mainTable, tcInfo, allTableSet);

        Table table = tcInfo.findTable(mainTable);
        String idColumn = table.idWhere(!allTableSet.isEmpty());
        List<String> idKey = table.getIdKey();
        StringJoiner sj = new StringJoiner(", ");
        for (Map<String, Object> idMap : idList) {
            if (idKey.size() > 1) {
                // WHERE ( id1, id2 ) IN ( (X, XX), (Y, YY) )
                StringJoiner innerJoiner = new StringJoiner(", ");
                for (String id : idKey) {
                    innerJoiner.add("?");
                    params.add(idMap.get(id));
                }
                sj.add("(" + innerJoiner + ")");
            } else {
                // WHERE id IN (x, y, z)
                sj.add("?");
                params.add(idMap.get(idKey.get(0)));
            }
        }
        return "SELECT " + selectColumn + " FROM " + from + " WHERE " + idColumn + " IN (" + sj + ")";
    }

    public static String toInnerSql(String selectColumn, String table, String relationColumn,
                                    List<Object> relationIds, List<Object> params) {
        StringJoiner in = new StringJoiner(", ");
        for (Object relationId : relationIds) {
            in.add("?");
            params.add(relationId);
        }
        return "SELECT " + selectColumn + " FROM " + table + " WHERE " + relationColumn + " IN" + " (" + in + ")";
    }
}
