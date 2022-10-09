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

    public static String toFromSql(SchemaColumnInfo scInfo, String mainSchema, List<SchemaJoinRelation> relationList) {
        StringBuilder sbd = new StringBuilder(" FROM ");
        Schema schema = scInfo.findSchema(mainSchema);
        String mainSchemaName = schema.getName();
        sbd.append(toSqlField(mainSchemaName));
        if (!relationList.isEmpty()) {
            sbd.append(" AS ").append(schema.getAlias());
            for (SchemaJoinRelation joinRelation : relationList) {
                sbd.append(joinRelation.generateJoin(scInfo));
            }
        }
        return sbd.toString();
    }

    public static String toWhereSql(SchemaColumnInfo scInfo, String mainSchema,
                                    boolean needAlias, ReqParam param, List<Object> params) {
        return param.generateWhereSql(mainSchema, scInfo, needAlias, params);
    }

    public static String toCountGroupSql(String selectSql) {
        return "SELECT COUNT(*) FROM ( " + selectSql + " ) TMP";
    }

    public static String toSelectGroupSql(SchemaColumnInfo scInfo, String fromAndWhere, String mainSchema,
                                          ReqResult result, Set<String> schemaSet, List<Object> params) {
        boolean needAlias = !schemaSet.isEmpty();
        String selectField = result.generateAllSelectSql(mainSchema, scInfo, schemaSet);
        boolean emptySelect = selectField.isEmpty();

        // SELECT ... FROM ... WHERE ... GROUP BY ... HAVING ...
        StringBuilder sbd = new StringBuilder("SELECT ");
        if (!emptySelect) {
            sbd.append(selectField);
        }
        String functionSql = result.generateFunctionSql(mainSchema, needAlias, scInfo);
        if (!functionSql.isEmpty()) {
            if (!emptySelect) {
                sbd.append(", ");
            }
            sbd.append(functionSql);
        }
        sbd.append(fromAndWhere);
        sbd.append(result.generateGroupSql(mainSchema, needAlias, scInfo));
        sbd.append(result.generateHavingSql(mainSchema, needAlias, scInfo, params));
        return sbd.toString();
    }

    public static String toCountWithoutGroupSql(SchemaColumnInfo scInfo, String mainSchema, boolean needAlias,
                                                boolean queryHasMany, String fromAndWhere) {
        if (queryHasMany) {
            // SELECT COUNT(DISTINCT xx.id) FROM ...
            String idSelect = scInfo.findSchema(mainSchema).idSelect(needAlias);
            return String.format("SELECT COUNT(DISTINCT %s) %s", idSelect, fromAndWhere);
        } else {
            return "SELECT COUNT(*) " + fromAndWhere;
        }
    }

    public static String toPageWithoutGroupSql(SchemaColumnInfo scInfo, String fromAndWhere, String mainSchema,
                                               ReqParam param, ReqResult result,
                                               Set<String> allSchemaSet, List<Object> params) {
        String selectField = result.generateAllSelectSql(mainSchema, scInfo, allSchemaSet);
        // SELECT ... FROM ... WHERE ... ORDER BY ... limit ...
        String orderSql = param.generateOrderSql(mainSchema, !allSchemaSet.isEmpty(), scInfo);
        return "SELECT " + selectField + fromAndWhere + orderSql + param.generatePageSql(params);
    }

    public static String toIdPageSql(SchemaColumnInfo scInfo, String fromAndWhere, String mainSchema,
                                     boolean needAlias, ReqParam param, List<Object> params) {
        String idSelect = scInfo.findSchema(mainSchema).idSelect(needAlias);
        // SELECT id FROM ... WHERE ... ORDER BY ... LIMIT ...
        String orderSql = param.generateOrderSql(mainSchema, needAlias, scInfo);
        return "SELECT " + idSelect + fromAndWhere + orderSql + param.generatePageSql(params);
    }
    public static String toSelectWithIdSql(SchemaColumnInfo scInfo, String mainSchema, String fromSql,
                                           ReqResult result, List<Map<String, Object>> idList,
                                           Set<String> allSchemaSet, List<Object> params) {
        // SELECT ... FROM ... WHERE id IN (x, y, z)
        String selectColumn = result.generateAllSelectSql(mainSchema, scInfo, allSchemaSet);

        Schema schema = scInfo.findSchema(mainSchema);
        String idWhere = schema.idWhere(!allSchemaSet.isEmpty());
        List<String> idKey = schema.getIdKey();
        StringJoiner sj = new StringJoiner(", ", "( ", " )");
        for (Map<String, Object> idMap : idList) {
            if (idKey.size() > 1) {
                // WHERE (id1, id2) IN ( (X, XX), (Y, YY) )
                StringJoiner innerJoiner = new StringJoiner(", ", "(", ")");
                for (String id : idKey) {
                    innerJoiner.add("?");
                    params.add(idMap.get(id));
                }
                sj.add(innerJoiner.toString());
            } else {
                // WHERE id IN (x, y, z)
                sj.add("?");
                params.add(idMap.get(idKey.get(0)));
            }
        }
        return String.format("SELECT %s FROM %s WHERE %s IN %s", selectColumn, fromSql, idWhere, sj);
    }
}
