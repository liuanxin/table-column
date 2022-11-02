package com.github.liuanxin.query.model;

import com.github.liuanxin.query.enums.ConditionType;
import com.github.liuanxin.query.enums.OperateType;
import com.github.liuanxin.query.util.QueryJsonUtil;
import com.github.liuanxin.query.util.QueryUtil;

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
 *     [ "name", "start", "abc" ],
 *     [ "gender", -- "eq", --  1 ],  -- eq 可以省略
 *     [ "age", "bet", [ 18, 40 ] ],
 *     [ "province", "in", [ "x", "y", "z" ] ],
 *     [ "city", "fuzzy", "xx" ],
 *     [ "time", "ge", "xxxx-xx-xx xx:xx:xx" ]
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
 *     [ "name", "start", "abc" ],
 *     {
 *       "operate": "or",
 *       "conditions": [
 *         [ "gender", 1 ],
 *         [ "age", "bet", [ 18, 40 ] ]
 *       ]
 *     },
 *     {
 *       "operate": "or",
 *       "conditions": [
 *         [ "province", "in", [ "x", "y", "z" ] ],
 *         [ "city", "fuzzy", "xx" ]
 *       ]
 *     },
 *     [ "time", "ge", "xxxx-xx-xx xx:xx:xx" ]
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
 *     [ "name", "end", "abc" ],
 *     [ "gender", 1 ],
 *     [ "age", "bet", [ 18, 40 ] ],
 *     [ "province", "in", [ "x", "y", "z" ] ],
 *     [ "city", "fuzzy", "xx" ],
 *     [ "time", "ge", "xxxx-xx-xx xx:xx:xx" ]
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
 *     [ "name", "start", "abc" ],
 *     {
 *       "conditions": [
 *         [ "gender", 1 ],
 *         [ "age", "bet", [ 18, 40 ] ]
 *       ]
 *     },
 *     {
 *       "conditions": [
 *         [ "province", "in", [ "x", "y", "z" ] ],
 *         [ "city", "fuzzy", "xx" ]
 *       ]
 *     },
 *     [ "time", "ge", "xxxx-xx-xx xx:xx:xx" ]
 *   ]
 * }
 * </pre>
 */
public class ReqParamOperate {

    /** 条件拼接类型: 并且(and) 和 或者(or) 两种, 不设置则默认是 and */
    private OperateType operate;
    /** 条件 */
    private List<Object> conditions;

    public ReqParamOperate() {}
    public ReqParamOperate(OperateType operate, List<Object> conditions) {
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
        if (!(o instanceof ReqParamOperate)) return false;
        ReqParamOperate that = (ReqParamOperate) o;
        return operate == that.operate && Objects.equals(conditions, that.conditions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operate, conditions);
    }

    @Override
    public String toString() {
        return "ReqParamOperate{operate=" + operate + ", conditions=" + conditions + '}';
    }


    public Set<String> checkCondition(String mainTable, TableColumnInfo tcInfo, int maxListCount) {
        if (QueryUtil.isEmpty(conditions)) {
            return Collections.emptySet();
        }

        Set<String> queryTableSet = new LinkedHashSet<>();
        for (Object condition : conditions) {
            if (condition != null) {
                if (condition instanceof List<?>) {
                    List<?> list = (List<?>) condition;
                    if (QueryUtil.isEmpty(list)) {
                        throw new RuntimeException("param condition(" + condition + ") can't be blank");
                    }
                    int size = list.size();
                    if (size < 2) {
                        throw new RuntimeException("param condition(" + condition + ") error");
                    }
                    String column = QueryUtil.toStr(list.get(0));
                    if (QueryUtil.isEmpty(column)) {
                        throw new RuntimeException("param condition(" + condition + ") column can't be blank");
                    }

                    Table sa = tcInfo.findTable(QueryUtil.getTableName(column, mainTable));
                    if (sa == null) {
                        throw new RuntimeException("param condition(" + condition + ") column has no table info");
                    }
                    queryTableSet.add(sa.getName());

                    boolean standardSize = (size == 2);
                    ConditionType type = standardSize ? ConditionType.EQ : ConditionType.deserializer(list.get(1));
                    if (type == null) {
                        throw new RuntimeException(String.format("param condition column(%s) need type", column));
                    }

                    TableColumn tableColumn = tcInfo.findTableColumn(sa, QueryUtil.getColumnName(column));
                    if (tableColumn == null) {
                        throw new RuntimeException(String.format("param condition column(%s) has no column info", column));
                    }
                    type.checkTypeAndValue(tableColumn.getFieldType(), column,
                            list.get(standardSize ? 1 : 2), tableColumn.getStrLen(), maxListCount);
                } else {
                    ReqParamOperate compose = QueryJsonUtil.convert(condition, ReqParamOperate.class);
                    if (compose == null) {
                        throw new RuntimeException("compose condition(" + condition + ") error");
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
            if (condition != null) {
                if (condition instanceof List<?>) {
                    List<?> list = (List<?>) condition;
                    if (QueryUtil.isNotEmpty(list)) {
                        int size = list.size();
                        String column = QueryUtil.toStr(list.get(0));

                        boolean standardSize = (size == 2);
                        ConditionType type = standardSize ? ConditionType.EQ : ConditionType.deserializer(list.get(1));
                        Object value = list.get(standardSize ? 1 : 2);

                        String tableName = QueryUtil.getTableName(column, mainTable);
                        String columnName = QueryUtil.getColumnName(column);
                        Class<?> columnType = tcInfo.findTableColumn(tableName, columnName).getFieldType();
                        String useColumn = QueryUtil.getQueryColumn(needAlias, column, mainTable, tcInfo);
                        StringBuilder print = new StringBuilder();
                        String sql = type.generateSql(useColumn, columnType, value, params, print);
                        if (QueryUtil.isNotEmpty(sql)) {
                            sj.add(sql);
                            printSj.add(print);
                        }
                    }
                } else {
                    ReqParamOperate compose = QueryJsonUtil.convert(condition, ReqParamOperate.class);
                    if (compose != null) {
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
}
