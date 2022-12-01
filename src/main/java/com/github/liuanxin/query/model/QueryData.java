package com.github.liuanxin.query.model;

import com.github.liuanxin.query.enums.ConditionType;
import com.github.liuanxin.query.enums.QueryOrder;
import com.github.liuanxin.query.enums.ResultGroup;
import com.github.liuanxin.query.function.FunctionSerialize;
import com.github.liuanxin.query.util.QueryLambdaUtil;
import com.github.liuanxin.query.util.QueryUtil;

import java.io.Serializable;
import java.util.*;

public class QueryData<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Class<T> clazz;
    private final List<QueryRelation> relationList;
    private final List<FunctionSerialize<?, ?>> selectList;
    private final Set<String> selectStrSet;
    private final Set<String> functionSet;
    private final ReqQuery query;
    private final List<FunctionSerialize<?, ?>> groupByList;
    private final Set<String> groupByStrList;
    private final Set<String> havingSet;
    private final Map<FunctionSerialize<?, ?>, QueryOrder> orderMap;
    private final Map<String, QueryOrder> orderStrMap;
    private final List<Integer> pageList;

    private QueryData(Class<T> clazz) {
        this.clazz = clazz;
        relationList = new ArrayList<>();
        selectList = new ArrayList<>();
        selectStrSet = new LinkedHashSet<>();
        functionSet = new LinkedHashSet<>();
        query = new ReqQuery();
        groupByList = new ArrayList<>();
        groupByStrList = new LinkedHashSet<>();
        havingSet = new LinkedHashSet<>();
        orderMap = new LinkedHashMap<>();
        orderStrMap = new LinkedHashMap<>();
        pageList = new ArrayList<>();
    }

    public void clear() {
        relationList.clear();
        selectList.clear();
        selectStrSet.clear();
        functionSet.clear();
        query.clear();
        groupByList.clear();
        groupByStrList.clear();
        havingSet.clear();
        orderMap.clear();
        orderStrMap.clear();
        pageList.clear();
    }

    public QueryData<T> addSelect(FunctionSerialize<?, ?> selectColumn) {
        return QueryUtil.isNull(selectColumn) ? this : addSelects(selectColumn);
    }
    public QueryData<T> addSelects(FunctionSerialize<?, ?>... selectColumns) {
        if (selectColumns != null && selectColumns.length > 0) {
            selectList.addAll(Arrays.asList(selectColumns));
        }
        return this;
    }
    public QueryData<T> addSelect(String selectColumn) {
        return QueryUtil.isNull(selectColumn) ? this : addSelects(selectColumn);
    }
    public QueryData<T> addSelects(String... selectColumns) {
        if (selectColumns != null && selectColumns.length > 0) {
            selectStrSet.addAll(Arrays.asList(selectColumns));
        }
        return this;
    }

    public QueryData<T> addFunction(String alias, ResultGroup func, FunctionSerialize<?, ?> functionColumn) {
        return QueryUtil.isNull(functionColumn) ? this : addFunctions(alias, func, functionColumn);
    }
    public QueryData<T> addFunctions(String alias, ResultGroup func, FunctionSerialize<?, ?>... functionColumns) {
        if (QueryUtil.isNotEmpty(alias) && QueryUtil.isNotNull(func) && functionColumns != null && functionColumns.length > 0) {
            List<String> funList = new ArrayList<>();
            for (FunctionSerialize<?, ?> column : functionColumns) {
                funList.add(QueryLambdaUtil.toColumnName(column));
            }
            functionSet.add(func.generateColumn(String.join(", ", funList), alias));
        }
        return this;
    }
    public QueryData<T> addFunction(String alias, ResultGroup func, String functionColumn) {
        return QueryUtil.isNull(functionColumn) ? this : addFunctions(alias, func, functionColumn);
    }
    public QueryData<T> addFunctions(String alias, ResultGroup func, String... functionColumns) {
        if (QueryUtil.isNotEmpty(alias) && QueryUtil.isNotNull(func) && functionColumns != null && functionColumns.length > 0) {
            functionSet.add(func.generateColumn(String.join(", ", functionColumns), alias));
        }
        return this;
    }

//    public QueryData<T> addJoin(JoinType joinType, Class<?> clazz, FunctionSerialize<?, ?> column)

    public QueryData<T> addCondition(FunctionSerialize<?, ?> column, ConditionType type, Object value) {
        if (QueryUtil.isNotNull(column) && QueryUtil.isNotNull(type)) {
            query.addCondition(column, type, value);
        }
        return this;
    }
    public QueryData<T> addCondition(String column, ConditionType type, Object value) {
        if (QueryUtil.isNotNull(column) && QueryUtil.isNotNull(type)) {
            query.addCondition(column, type, value);
        }
        return this;
    }
    public QueryData<T> addComposeCondition(ReqQuery composeCondition) {
        if (QueryUtil.isNotNull(composeCondition)) {
            query.addComposeCondition(composeCondition);
        }
        return this;
    }

    public QueryData<T> addGroupBy(FunctionSerialize<?, ?> groupBy) {
        return QueryUtil.isNull(groupBy) ? this : addGroupBys(groupBy);
    }
    public QueryData<T> addGroupBys(FunctionSerialize<?, ?>... groupBys) {
        if (groupBys != null && groupBys.length > 0) {
            groupByList.addAll(Arrays.asList(groupBys));
        }
        return this;
    }
    public QueryData<T> addGroupBy(String groupBy) {
        return QueryUtil.isNull(groupBy) ? this : addGroupBys(groupBy);
    }
    public QueryData<T> addGroupBys(String... groupBys) {
        if (groupBys != null && groupBys.length > 0) {
            groupByStrList.addAll(Arrays.asList(groupBys));
        }
        return this;
    }

    public QueryData<T> addHaving(String alias, ConditionType type, Object value) {
        havingSet.add(alias + type.getValue());
        return this;
    }

    public QueryData<T> addOrder(FunctionSerialize<?, ?> column, QueryOrder order) {
        if (QueryUtil.isNotNull(column) && QueryUtil.isNotNull(order)) {
            orderMap.put(column, order);
        }
        return this;
    }
    public QueryData<T> addOrder(List<FunctionSerialize<?, ?>> columns, QueryOrder order) {
        if (QueryUtil.isNotEmpty(columns) && QueryUtil.isNotNull(order)) {
            for (FunctionSerialize<?, ?> column : columns) {
                if (QueryUtil.isNotNull(column)) {
                    orderMap.put(column, order);
                }
            }
        }
        return this;
    }

    public QueryData<T> addOrder(String column, QueryOrder order) {
        if (QueryUtil.isNotNull(column) && QueryUtil.isNotNull(order)) {
            orderStrMap.put(column, order);
        }
        return this;
    }
    public QueryData<T> addOrder(QueryOrder order, String... columns) {
        if (columns != null && columns.length > 0 && QueryUtil.isNotNull(order)) {
            for (String column : columns) {
                if (QueryUtil.isNotNull(column)) {
                    orderStrMap.put(column, order);
                }
            }
        }
        return this;
    }

    public QueryData<T> withOne() {
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


    public ReqInfo toQuery() {
        return null;
    }

    public static <T> QueryData<T> create(Class<T> clazz) {
        return new QueryData<>(clazz);
    }
}
