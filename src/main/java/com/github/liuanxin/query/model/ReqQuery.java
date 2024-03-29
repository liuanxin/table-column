package com.github.liuanxin.query.model;

import com.github.liuanxin.query.enums.ConditionType;
import com.github.liuanxin.query.enums.OperateType;
import com.github.liuanxin.query.function.FunctionSerialize;
import com.github.liuanxin.query.util.QueryJsonUtil;
import com.github.liuanxin.query.util.QueryLambdaUtil;
import com.github.liuanxin.query.util.QueryUtil;

import java.io.Serializable;
import java.util.*;

/**
 * <pre>
 * name like 'abc%'
 * and gender = 1
 * and age between 18 and 40
 * and province in ( 'x', 'y', 'z' )
 * and city like '%xx%'
 * and time >= 'xxxx-xx-xx xx:xx:xx'
 * {
 *   -- "operate": "and",    -- 并且(and) 和 或者(or) 两种, 不设置则默认是 and
 *   "conditions": [
 *     [ "name", "$start", "abc" ],
 *     [ "gender", "$eq", 1 ],
 *     [ "age", "$bet", [ 18, 40 ] ],
 *     [ "province", "$in", [ "x", "y", "z" ] ],
 *     [ "city", "$fuzzy", "xx" ],
 *     [ "time", "$ge", "xxxx-xx-xx xx:xx:xx" ]
 *   ]
 * }
 *
 *
 * name like 'abc%'
 * and ( gender = 1 or age between 18 and 40 )
 * and ( province in ( 'x', 'y', 'z' ) or city like '%xx%' )
 * and time >= 'xxxx-xx-xx xx:xx:xx'
 * {
 *   "conditions": [
 *     [ "name", "$start", "abc" ],
 *     {
 *       "operate": "or",
 *       "conditions": [
 *         [ "gender", "$eq", 1 ],
 *         [ "age", "$bet", [ 18, 40 ] ]
 *       ]
 *     },
 *     {
 *       "operate": "or",
 *       "conditions": [
 *         [ "province", "$in", [ "x", "y", "z" ] ],
 *         [ "city", "$fuzzy", "xx" ]
 *       ]
 *     },
 *     [ "time", "$ge", "xxxx-xx-xx xx:xx:xx" ]
 *   ]
 * }
 *
 *
 * name like '%abc'
 * or gender = 1
 * or age between 18 and 40
 * or province in ( 'x', 'y', 'z' )
 * or city like '%xx%'
 * or time >= 'xxxx-xx-xx xx:xx:xx'
 * {
 *   "operate": "or",
 *   "conditions": [
 *     [ "name", "$end", "abc" ],
 *     [ "gender", "$eq", 1 ],
 *     [ "age", "$bet", [ 18, 40 ] ],
 *     [ "province", "$in", [ "x", "y", "z" ] ],
 *     [ "city", "$fuzzy", "xx" ],
 *     [ "time", "$ge", "xxxx-xx-xx xx:xx:xx" ]
 *   ]
 * }
 *
 *
 * name like 'abc%'
 * or ( gender = 1 and age between 18 and 40 )
 * or ( province in ( 'x', 'y', 'z' ) and city like '%xx%' )
 * or time >= 'xxxx-xx-xx xx:xx:xx'
 * {
 *   "operate": "or",
 *   "conditions": [
 *     [ "name", "$start", "abc" ],
 *     {
 *       "conditions": [
 *         [ "gender", "$eq", 1 ],
 *         [ "age", "$bet", [ 18, 40 ] ]
 *       ]
 *     },
 *     {
 *       "conditions": [
 *         [ "province", "$in", [ "x", "y", "z" ] ],
 *         [ "city", "$fuzzy", "xx" ]
 *       ]
 *     },
 *     [ "time", "$ge", "xxxx-xx-xx xx:xx:xx" ]
 *   ]
 * }
 * </pre>
 */
public class ReqQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 条件拼接类型: 并且(and) 和 或者(or) 两种, 不设置则默认是 and */
    private OperateType operate;
    /** 条件 */
    private List<Object> conditions;

    public ReqQuery() {}
    public ReqQuery(OperateType operate, List<Object> conditions) {
        this.operate = operate;
        this.conditions = conditions;
    }

    public OperateType getOperate() {
        return operate;
    }
    public void setOperate(OperateType operate) {
        this.operate = operate;
    }

    public List<Object> getConditions() {
        return conditions;
    }
    public void setConditions(List<Object> conditions) {
        this.conditions = conditions;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReqQuery reqQuery = (ReqQuery) o;
        return operate == reqQuery.operate && Objects.equals(conditions, reqQuery.conditions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operate, conditions);
    }

    @Override
    public String toString() {
        return "ReqQuery{" +
                "operate=" + operate +
                ", conditions=" + conditions +
                '}';
    }


    public Set<String> checkCondition(String mainTable, TableColumnInfo tcInfo, int maxListCount) {
        if (QueryUtil.isEmpty(conditions)) {
            return Collections.emptySet();
        }

        Set<String> queryTableSet = new LinkedHashSet<>();
        for (Object condition : conditions) {
            if (QueryUtil.isNotNull(condition)) {
                if (condition instanceof List<?>) {
                    List<?> list = (List<?>) condition;
                    if (QueryUtil.isEmpty(list)) {
                        throw new RuntimeException("param: condition(" + condition + ") can't be blank");
                    }
                    int size = list.size();
                    if (size < 2) {
                        throw new RuntimeException("param: condition(" + condition + ") error");
                    }
                    String columnAlias = QueryUtil.toStr(list.get(0));
                    if (QueryUtil.isEmpty(columnAlias)) {
                        throw new RuntimeException("param: condition(" + condition + ") column can't be blank");
                    }

                    Table table = tcInfo.findTableWithAlias(QueryUtil.getTableName(columnAlias, mainTable));
                    if (QueryUtil.isNull(table)) {
                        throw new RuntimeException("param: condition(" + condition + ") column has no table info");
                    }
                    queryTableSet.add(table.getName());

                    ConditionType type = ConditionType.deserializer(list.get(1));
                    if (QueryUtil.isNull(type)) {
                        throw new RuntimeException(String.format("param: condition column(%s) type(%s) error", columnAlias, list.get(1)));
                    }

                    TableColumn tableColumn = tcInfo.findTableColumnWithAlias(table, QueryUtil.getColumnName(columnAlias));
                    if (QueryUtil.isNull(tableColumn)) {
                        throw new RuntimeException(String.format("param: condition column(%s) has no column info", columnAlias));
                    }
                    type.checkTypeAndValue(tableColumn.getFieldType(), columnAlias,
                            ((size > 2) ? list.get(2) : null), tableColumn.getStrLen(), maxListCount);
                } else {
                    ReqQuery compose = QueryJsonUtil.convert(condition, ReqQuery.class);
                    if (QueryUtil.isNull(compose)) {
                        throw new RuntimeException("param: compose condition(" + condition + ") error");
                    }
                    compose.checkCondition(mainTable, tcInfo, maxListCount);
                }
            }
        }
        return queryTableSet;
    }

    public String generateSql(String mainTable, TableColumnInfo tcInfo, boolean needAlias,
                              List<Object> params, StringBuilder printSql) {
        // noinspection DuplicatedCode
        if (QueryUtil.isEmpty(conditions)) {
            return "";
        }

        String operateType = (QueryUtil.isNull(operate) ? OperateType.AND : operate).name().toUpperCase();
        StringJoiner sj = new StringJoiner(" " + operateType + " ");
        StringJoiner printSj = new StringJoiner(" " + operateType + " ");
        for (Object condition : conditions) {
            if (QueryUtil.isNotNull(condition)) {
                if (condition instanceof List<?>) {
                    List<?> list = (List<?>) condition;
                    if (QueryUtil.isNotEmpty(list)) {
                        int size = list.size();
                        String column = QueryUtil.toStr(list.get(0));

                        ConditionType type = ConditionType.deserializer(list.get(1));
                        Object value = (size > 2) ? list.get(2) : null;

                        String tableName = QueryUtil.getTableName(column, mainTable);
                        String columnName = QueryUtil.getColumnName(column);
                        Class<?> fieldType = tcInfo.findTableColumn(tableName, columnName).getFieldType();
                        String useColumn = QueryUtil.getQueryColumn(needAlias, column, mainTable, tcInfo);
                        StringBuilder print = new StringBuilder();
                        String sql = type.generateSql(useColumn, fieldType, value, params, print);
                        if (QueryUtil.isNotEmpty(sql)) {
                            sj.add(sql);
                            printSj.add(print);
                        }
                    }
                } else {
                    ReqQuery compose = QueryJsonUtil.convert(condition, ReqQuery.class);
                    if (QueryUtil.isNotNull(compose)) {
                        StringBuilder print = new StringBuilder();
                        String innerWhereSql = compose.generateSql(mainTable, tcInfo, needAlias, params, print);
                        if (QueryUtil.isNotEmpty(innerWhereSql)) {
                            sj.add("( " + innerWhereSql + " )");
                            printSj.add("( " + print + " )");
                        }
                    }
                }
            }
        }
        if (sj.length() == 0) {
            return "";
        } else {
            printSql.append(printSj);
            return sj.toString().trim();
        }
    }


    public void clear() {
        if (QueryUtil.isNotNull(operate)) {
            operate = null;
        }
        if (QueryUtil.isNotEmpty(conditions)) {
            conditions.clear();
        }
    }

    public <T> void addCondition(FunctionSerialize<T,?> column, ConditionType type, Object value) {
        if (QueryUtil.isNotEmpty(conditions)) {
            String c = QueryLambdaUtil.toTableName(column) + "." + QueryLambdaUtil.toColumnName(column);
            conditions.add(Arrays.asList(c, type.name().toLowerCase(), value));
        }
    }
    public void addCondition(String column, ConditionType type, Object value) {
        if (QueryUtil.isNotEmpty(conditions)) {
            conditions.add(Arrays.asList(column, type.name().toLowerCase(), value));
        }
    }

    public void addComposeCondition(ReqQuery composeCondition) {
        if (QueryUtil.isNotEmpty(conditions)) {
            conditions.add(composeCondition);
        }
    }

    public static ReqQuery buildId(String idField, Serializable id) {
        return new ReqQuery(null, Collections.singletonList(
                Arrays.asList(idField, id)
        ));
    }

    public static ReqQuery buildIds(String idField, List<Serializable> ids) {
        return new ReqQuery(null, Collections.singletonList(
                Arrays.asList(idField, ConditionType.$IN, ids)
        ));
    }
}
