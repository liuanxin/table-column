package com.github.liuanxin.query.model;

import com.github.liuanxin.query.enums.ConditionType;
import com.github.liuanxin.query.enums.OperateType;
import com.github.liuanxin.query.util.QueryJsonUtil;
import com.github.liuanxin.query.util.QueryUtil;

import java.io.Serializable;
import java.util.*;

/**
 * <pre>
 * name like 'abc%'   and gender = 1   and age between 18 and 40
 * and province in ( 'x', 'y', 'z' )   and city like '%xx%'   and time >= now()
 * {
 *   -- "operate": "and",    -- 并且(and) 和 或者(or) 两种, 不设置则默认是 and
 *   "conditions": [
 *     [ "name", "rl", "abc" ],
 *     [ "gender", -- "eq", --  1 ],  -- eq 可以省略
 *     [ "age", "bet", [ 18, 40 ] ],
 *     [ "province", "in", [ "x", "y", "z" ] ],
 *     [ "city", "like", "xx" ],
 *     [ "time", "ge", "xxxx-xx-xx xx:xx:xx" ]
 *   ]
 * }
 *
 *
 * name like 'abc%'   and ( gender = 1 or age between 18 and 40 )
 * and ( province in ( 'x', 'y', 'z' ) or city like '%xx%' )   and time >= now()
 * {
 *   "conditions": [
 *     [ "name", "rl", "abc" ],
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
 *         [ "city", "like", "xx" ]
 *       ]
 *     },
 *     [ "time", "ge", "xxxx-xx-xx xx:xx:xx" ]
 *   ]
 * }
 *
 *
 * name like 'abc%'   or gender = 1   or age between 18 and 40
 * or province in ( 'x', 'y', 'z' )   or city like '%xx%'   or time >= now()
 * {
 *   "operate": "or",
 *   "conditions": [
 *     [ "name", "rl", "abc" ],
 *     [ "gender", 1 ],
 *     [ "age", "bet", [ 18, 40 ] ],
 *     [ "province", "in", [ "x", "y", "z" ] ],
 *     [ "city", "like", "xx" ],
 *     [ "time", "ge", "xxxx-xx-xx xx:xx:xx" ]
 *   ]
 * }
 *
 *
 * name like 'abc%'   or time >= now()
 * or ( gender = 1 and age between 18 and 40 )
 * or ( province in ( 'x', 'y', 'z' ) and city like '%xx%' )
 * {
 *   "operate": "or",
 *   "conditions": [
 *     [ "name", "rl", "abc" ],
 *     [ "time", "ge", "xxxx-xx-xx xx:xx:xx" ],
 *     {
 *       "conditions": [
 *         [ "gender", 1 ],
 *         [ "age", "bet", [ 18, 40 ] ]
 *       ]
 *     },
 *     {
 *       "conditions": [
 *         [ "province", "in", [ "x", "y", "z" ] ],
 *         [ "city", "like", "xx" ]
 *       ]
 *     }
 *   ]
 * }
 * </pre>
 */
public class SingleSchemaWhere {

    /** 条件拼接类型: 并且(and) 和 或者(or) 两种, 不设置则默认是 and */
    private OperateType operate;
    /** 条件 */
    private List<Object> conditions;

    public SingleSchemaWhere() {}
    public SingleSchemaWhere(OperateType operate, List<Object> conditions) {
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
        if (!(o instanceof SingleSchemaWhere)) return false;
        SingleSchemaWhere that = (SingleSchemaWhere) o;
        return operate == that.operate && Objects.equals(conditions, that.conditions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operate, conditions);
    }

    @Override
    public String toString() {
        return "SingleSchemaWhere{" +
                "operate=" + operate +
                ", conditions=" + conditions +
                '}';
    }


    public static SingleSchemaWhere buildId(String idField, Serializable id) {
        return new SingleSchemaWhere(null, Collections.singletonList(
                Arrays.asList(idField, id)
        ));
    }

    public static SingleSchemaWhere buildIds(String idField, List<Serializable> ids) {
        return new SingleSchemaWhere(null, Collections.singletonList(
                Arrays.asList(idField, ConditionType.IN, ids)
        ));
    }

    public String generateSql(String schema, SchemaColumnInfo scInfo, List<Object> params) {
        if (conditions == null || conditions.isEmpty()) {
            return "";
        }

        String operateType = (operate == null ? OperateType.AND : operate).name().toUpperCase();
        StringJoiner sj = new StringJoiner(" " + operateType + " ");
        for (Object condition : conditions) {
            if (condition != null) {
                if (condition instanceof List<?>) {
                    List<?> list = (List<?>) condition;
                    if (!list.isEmpty()) {
                        String column = QueryUtil.toStr(list.get(0));

                        boolean standardSize = (list.size() == 2);
                        Object value = list.get(standardSize ? 1 : 2);
                        ConditionType type = standardSize ? ConditionType.EQ : ConditionType.deserializer(list.get(1));

                        Class<?> columnType = scInfo.findSchemaColumn(schema, column).getColumnType();
                        String useColumn = QueryUtil.getUseColumn(false, column, schema, scInfo);
                        String sql = type.generateSql(useColumn, columnType, value, params);
                        if (!sql.isEmpty()) {
                            sj.add(sql);
                        }
                    }
                } else {
                    ReqParamOperate compose = QueryJsonUtil.convert(condition, ReqParamOperate.class);
                    if (compose != null) {
                        String innerWhereSql = compose.generateSql(schema, scInfo, false, params);
                        if (!innerWhereSql.isEmpty()) {
                            sj.add("( " + innerWhereSql + " )");
                        }
                    }
                }
            }
        }
        return sj.toString().trim();
    }
}
