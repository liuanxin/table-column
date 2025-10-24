package com.github.liuanxin.query.util;

import com.github.liuanxin.query.constant.QueryConst;
import com.github.liuanxin.query.model.*;

import java.time.temporal.TemporalAccessor;
import java.util.*;

public class QuerySqlUtil {

    public static String getTableSql(String dialect) {
        if ("postgresql".equalsIgnoreCase(dialect)) {
            // relname 是 pg_class 中表的名称, description 是 pg_description 中的注释内容, relkind = 'r' 表示普通表 (regular table)
            // NOT IN 用于排除系统模式, 获取当前数据库中所有用户创建的表
            return "SELECT c.relname AS tn, d.description AS tc " +
                    "FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace " +
                    "LEFT JOIN pg_description d ON d.objoid = c.oid AND d.objsubid = 0 " +
                    "WHERE c.relkind = 'r' AND n.nspname NOT IN ('pg_catalog', 'information_schema')";
        } else /* if ("mysql".equalsIgnoreCase(dialect)) */ {
            return "SELECT `TABLE_NAME` tn, `TABLE_COMMENT` tc " +
                    "FROM `information_schema`.`TABLES` " +
                    "WHERE `TABLE_SCHEMA` = DATABASE()";
        }
    }

    public static String getColumnSql(String dialect) {
        if ("postgresql".equalsIgnoreCase(dialect)) {
            return "SELECT " +
                    "    c.table_name AS tn, c.column_name AS cn, c.data_type AS ct, d.description AS cc, " +
                    "    CASE WHEN pk.is_primary_key IS NOT NULL THEN 'PRI' ELSE '' END AS ck, " +
                    "    c.character_maximum_length AS cml, c.is_nullable AS ine, " +
                    "    CASE WHEN a.attidentity = 'a' or a.attidentity = 'd' or (c.column_default IS NOT NULL AND c.column_default LIKE 'nextval(%') THEN 'auto_increment' ELSE '' END AS ext, " +
                    "    c.column_default AS cd " +
                    "FROM information_schema.columns c " +
                    "JOIN pg_catalog.pg_class tbl ON tbl.relname = c.table_name AND tbl.relkind = 'r' " +
                    "JOIN pg_catalog.pg_attribute a ON a.attrelid = tbl.oid AND a.attname = c.column_name " +
                    "LEFT JOIN pg_catalog.pg_description d ON d.objoid = tbl.oid AND d.objsubid = c.ordinal_position " +
                    "LEFT JOIN ( " +
                    "    SELECT kcu.table_schema, kcu.table_name, kcu.column_name, 'PRI' AS is_primary_key " +
                    "    FROM information_schema.table_constraints tc JOIN information_schema.key_column_usage kcu " +
                    "    ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema AND tc.table_name = kcu.table_name " +
                    "    WHERE tc.constraint_type = 'PRIMARY KEY' " +
                    ") pk ON pk.table_schema = c.table_schema AND pk.table_name = c.table_name AND pk.column_name = c.column_name " +
                    "WHERE c.table_schema NOT IN ('pg_catalog', 'information_schema') " +
                    "ORDER BY c.table_name, c.ordinal_position";
        } else /* if ("mysql".equalsIgnoreCase(dialect)) */ {
            // ck 是 PRI 则表示是主键列, ext 是 auto_increment 表示是自增列, cd 是列的默认值(目前用来判断此列是否有默认值)
            return "SELECT `TABLE_NAME` tn, `COLUMN_NAME` cn, `COLUMN_TYPE` ct, " +
                    "`COLUMN_COMMENT` cc, `COLUMN_KEY` ck, `CHARACTER_MAXIMUM_LENGTH` cml, " +
                    "`IS_NULLABLE` ine, `EXTRA` ext, `COLUMN_DEFAULT` cd " +
                    "FROM `information_schema`.`COLUMNS` " +
                    "WHERE `TABLE_SCHEMA` = DATABASE() " +
                    "ORDER BY `TABLE_NAME`, `ORDINAL_POSITION`";
        }
    }

    public static String toSqlField(String field) {
        return MysqlKeyWordUtil.hasKeyWord(field) || QueryUtil.isLong(field) ? ("`" + field + "`") : field;
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
        } else if (TemporalAccessor.class.isAssignableFrom(type)) {
            return QueryUtil.toLocalDate(value);
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
            return "'" + QueryUtil.format(QueryUtil.toDate(value)).replace("'", "''") + "'";
        } else if (TemporalAccessor.class.isAssignableFrom(type)) {
            return "'" + QueryUtil.format(QueryUtil.toLocalDate(value)).replace("'", "''") + "'";
        } else {
            return "'" + QueryUtil.toStr(value).replace("'", "''") + "'";
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
                                          String mainTable, ReqResult result, boolean needAlias, boolean force,
                                          boolean hasDistinct, List<Object> params, StringBuilder printSql) {
        String selectField = result.generateAllSelectSql(mainTable, tcInfo, needAlias, force);
        boolean emptySelect = QueryUtil.isEmpty(selectField);

        String distinct = hasDistinct ? "DISTINCT " : "";
        // SELECT ... FROM ... WHERE ... GROUP BY ... HAVING ...
        StringBuilder sbd = new StringBuilder();
        sbd.append("SELECT ").append(distinct);
        printSql.append("SELECT ").append(distinct);
        if (!emptySelect) {
            sbd.append(selectField);
            printSql.append(selectField);
        }

        String functionSql = result.generateFunctionSql(mainTable, needAlias, tcInfo, force);
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
                                                boolean hasDistinct, StringBuilder printSql) {
        if (hasDistinct) {
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
                                   boolean force, List<Object> params, boolean hasDistinct, StringBuilder printSql) {
        String selectColumn = result.generateAllSelectSql(mainTable, tcInfo, needAlias, force);
        if (QueryUtil.isEmpty(selectColumn)) {
            return "";
        }
        // SELECT ... FROM ... WHERE ... ORDER BY ... limit ...
        return toAppendSql(tcInfo, fromAndWhere, fromAndWherePrint, mainTable, param, needAlias,
                selectColumn, params, hasDistinct, printSql);
    }

    private static String toAppendSql(TableColumnInfo tcInfo, String fromAndWhere, String fromAndWherePrint,
                                      String mainTable, ReqParam param, boolean needAlias, String selectColumn,
                                      List<Object> params, boolean hasDistinct, StringBuilder printSql) {
        String orderSql = param.generateOrderSql(mainTable, needAlias, tcInfo);
        StringBuilder pagePrint = new StringBuilder();
        String pageSql = param.generatePageSql(params, pagePrint);
        if (hasDistinct) {
            StringBuilder appendOrder = new StringBuilder();
            String os = orderSql.trim();
            for (String order : os.substring("ORDER BY ".length()).split(",")) {
                String or;
                String str = order.toLowerCase();
                if (str.endsWith(" asc")) {
                    or = order.substring(0, order.length() - " asc".length());
                } else if (str.endsWith(" desc")) {
                    or = order.substring(0, order.length() - " desc".length());
                } else {
                    or = order;
                }
                if (!selectColumn.contains(or.trim())) {
                    appendOrder.append(", ").append(or.trim());
                }
            }
            printSql.append("SELECT DISTINCT ").append(selectColumn).append(appendOrder).append(fromAndWherePrint).append(orderSql).append(pagePrint);
            return "SELECT DISTINCT " + selectColumn + appendOrder + fromAndWhere + orderSql + pageSql;
        } else {
            printSql.append("SELECT ").append(selectColumn).append(fromAndWherePrint).append(orderSql).append(pagePrint);
            return "SELECT " + selectColumn + fromAndWhere + orderSql + pageSql;
        }
    }

    public static String toIdPageSql(TableColumnInfo tcInfo, String fromAndWhere, String fromAndWherePrint, String mainTable,
                                     boolean needAlias, ReqParam param, List<Object> params, boolean hasDistinct, StringBuilder printSql) {
        String idSelect = tcInfo.findTable(mainTable).idSelect(needAlias);
        // SELECT id FROM ... WHERE ... ORDER BY ... LIMIT ...
        return toAppendSql(tcInfo, fromAndWhere, fromAndWherePrint, mainTable, param, needAlias, idSelect, params, hasDistinct, printSql);
    }
    public static String toSelectWithIdSql(TableColumnInfo tcInfo, String mainTable, String fromSql,
                                           ReqResult result, List<Map<String, Object>> idList, boolean needAlias,
                                           boolean force, List<Object> params, StringBuilder printSql) {
        // SELECT ... FROM ... WHERE id IN (x, y, z)
        String selectColumn = result.generateAllSelectSql(mainTable, tcInfo, needAlias, force);
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
                sj.add("( " + innerJoiner + " )");
                print.add("( " + innerPrint + " )");
            } else {
                // WHERE id IN (x, y, z)
                Object data = idMap.get(idKey.get(0));
                sj.add("?");
                print.add(QuerySqlUtil.toPrintValue(data.getClass(), data));
                params.add(data);
            }
        }
        printSql.append("SELECT ").append(selectColumn).append(fromSql).append(" WHERE ").append(idColumn).append(" IN (").append(print).append(")");
        return "SELECT " + selectColumn + fromSql + " WHERE " + idColumn + " IN (" + sj + ")";
    }

    public static String toInnerSql(String selectColumn, String table, String relationColumn, List<Object> relationIds,
                                    List<Object> params, StringBuilder printSql, String logicDelete) {
        StringJoiner in = new StringJoiner(", ");
        StringJoiner print = new StringJoiner(", ");
        for (Object relationId : relationIds) {
            in.add("?");
            print.add(QuerySqlUtil.toPrintValue(relationId.getClass(), relationId));
            params.add(relationId);
        }
        printSql.append("SELECT ").append(selectColumn).append(" FROM ").append(table).append(" WHERE ");
        boolean emptyLogic = QueryUtil.isEmpty(logicDelete);
        if (emptyLogic) {
            printSql.append(relationColumn).append(" IN (").append(print).append(")");
        } else {
            printSql.append("( ").append(relationColumn).append(" IN (").append(print).append(")").append(" )").append(logicDelete);
        }
        return "SELECT " + selectColumn + " FROM " + table + " WHERE "
                + (emptyLogic ? (relationColumn + " IN" + " (" + in + ")") : ("( " + relationColumn + " IN" + " (" + in + ")" + " )" + logicDelete));
    }
}
