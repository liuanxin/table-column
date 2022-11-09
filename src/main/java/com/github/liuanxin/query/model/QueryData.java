package com.github.liuanxin.query.model;

import com.github.liuanxin.query.enums.ConditionType;
import com.github.liuanxin.query.enums.QueryOrder;
import com.github.liuanxin.query.enums.ResultGroup;
import com.github.liuanxin.query.function.FunctionSerialize;
import com.github.liuanxin.query.util.QueryLambdaUtil;
import com.github.liuanxin.query.util.QueryUtil;

import java.util.*;

public class QueryData<T> {

    private final Class<T> clazz;
    private final List<FunctionSerialize<T,?>> selectList;
    private final Set<String> functionSet;
    private final ParamWhere where;
    private final List<FunctionSerialize<T,?>> groupByList;
    private final Set<String> havingSet;
    private final Map<FunctionSerialize<T,?>, QueryOrder> orderMap;
    private final List<Integer> pageList;
    private final StringBuilder printSql;

    private QueryData(Class<T> clazz) {
        this.clazz = clazz;
        selectList = new ArrayList<>();
        functionSet = new LinkedHashSet<>();
        where = new ParamWhere();
        groupByList = new ArrayList<>();
        havingSet = new LinkedHashSet<>();
        orderMap = new LinkedHashMap<>();
        pageList = new ArrayList<>();
        printSql = new StringBuilder();
    }

    public void clear() {
        selectList.clear();
        functionSet.clear();
        where.clear();
        groupByList.clear();
        havingSet.clear();
        orderMap.clear();
        pageList.clear();
        printSql.setLength(0);
    }

    public QueryData<T> addSelect(FunctionSerialize<T,?> selectColumn) {
        return QueryUtil.isNull(selectColumn) ? this : addSelect(Collections.singletonList(selectColumn));
    }
    public QueryData<T> addSelect(List<FunctionSerialize<T,?>> selectColumns) {
        if (QueryUtil.isNotEmpty(selectColumns)) {
            selectList.addAll(selectColumns);
        }
        return this;
    }

    public QueryData<T> addFunction(String alias, ResultGroup func, FunctionSerialize<T,?> functionColumn) {
        return QueryUtil.isNull(functionColumn) ? this : addFunction(alias, func, Collections.singletonList(functionColumn));
    }
    public QueryData<T> addFunction(String alias, ResultGroup func, List<FunctionSerialize<T,?>> functionColumns) {
        if (QueryUtil.isNotEmpty(alias) && QueryUtil.isNotNull(func) && QueryUtil.isNotEmpty(functionColumns)) {
            List<String> funList = new ArrayList<>();
            for (FunctionSerialize<T, ?> column : functionColumns) {
                funList.add(QueryLambdaUtil.toColumnName(column));
            }
            functionSet.add(func.generateColumn(String.join(", ", funList), alias));
        }
        return this;
    }

    public QueryData<T> addCondition(FunctionSerialize<T,?> column, ConditionType type, Object value) {
        if (QueryUtil.isNotNull(column) && QueryUtil.isNotNull(type)) {
            where.addCondition(column, type, value);
        }
        return this;
    }
    public QueryData<T> addComposeCondition(ParamWhere composeCondition) {
        if (QueryUtil.isNotNull(composeCondition)) {
            where.addComposeCondition(composeCondition);
        }
        return this;
    }

    public QueryData<T> addGroupBy(FunctionSerialize<T,?> groupBy) {
        return QueryUtil.isNull(groupBy) ? this : addGroupBy(Collections.singletonList(groupBy));
    }
    public QueryData<T> addGroupBy(List<FunctionSerialize<T,?>> groupBys) {
        if (QueryUtil.isNotEmpty(groupBys)) {
            groupByList.addAll(groupBys);
        }
        return this;
    }

    public QueryData<T> addHaving(String alias, ConditionType type, Object value) {
        havingSet.add(alias + type.getValue());
        return this;
    }

    public QueryData<T> addOrder(FunctionSerialize<T,?> column, QueryOrder order) {
        if (QueryUtil.isNotNull(column) && QueryUtil.isNotNull(order)) {
            orderMap.put(column, order);
        }
        return this;
    }
    public QueryData<T> addOrder(List<FunctionSerialize<T,?>> columns, QueryOrder order) {
        if (QueryUtil.isNotEmpty(columns) && QueryUtil.isNotNull(order)) {
            for (FunctionSerialize<T, ?> column : columns) {
                if (QueryUtil.isNotNull(column)) {
                    orderMap.put(column, order);
                }
            }
        }
        return this;
    }

    public QueryData<T> withOne(int limit) {
        return withPage(1, 1);
    }
    public QueryData<T> withLimit(int limit) {
        return withPage(1, limit);
    }
    public QueryData<T> withPage(int page, int limit) {
        if (page > 0 && limit > 0) {
            pageList.clear();
            pageList.add(page);
            pageList.add(limit);
        }
        return this;
    }

    public String toSql(TableColumnInfo tcInfo, List<Object> params) {
        Table table = tcInfo.findTableByClass(clazz);
        if (QueryUtil.isNull(table)) {
            throw new RuntimeException("no ({" + clazz + "}) table has defined");
        }
        return null;
    }

    /** 请在 toSql 之后执行 */
    public String toPrintSql() {
        return printSql.toString();
    }

    public static <T> QueryData<T> create(Class<T> clazz) {
        return new QueryData<>(clazz);
    }
}
