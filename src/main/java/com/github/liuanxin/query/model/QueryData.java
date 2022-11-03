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
    private final SingleTableWhere where;
    private final List<FunctionSerialize<T,?>> groupByList;
    private final Set<String> havingSet;
    private final Map<FunctionSerialize<T,?>, QueryOrder> orderMap;
    private final List<Integer> pageList;

    private QueryData(Class<T> clazz) {
        this.clazz = clazz;
        selectList = new ArrayList<>();
        functionSet = new LinkedHashSet<>();
        where = new SingleTableWhere();
        groupByList = new ArrayList<>();
        havingSet = new LinkedHashSet<>();
        orderMap = new LinkedHashMap<>();
        pageList = new ArrayList<>();
    }

    public void clear() {
        selectList.clear();
        functionSet.clear();
        where.clear();
        groupByList.clear();
        havingSet.clear();
        orderMap.clear();
        pageList.clear();
    }

    public QueryData<T> addSelect(FunctionSerialize<T,?> select) {
        selectList.add(select);
        return this;
    }

    public QueryData<T> addFunction(String alias, ResultGroup func, FunctionSerialize<T,?>... functions) {
        if (QueryUtil.isNotNull(func) && QueryUtil.isNotNull(functions) && functions.length > 0) {
            List<String> funList = new ArrayList<>();
            for (FunctionSerialize<T, ?> fun : functions) {
                funList.add(QueryLambdaUtil.toColumnName(fun));
            }
            functionSet.add(func.generateColumn(alias));
        }
        return this;
    }

    public QueryData<T> addWhere(FunctionSerialize<T,?> column, ConditionType type, Object value) {
        where.addCondition(column, type, value);
        return this;
    }
    public QueryData<T> addComposeWhere(SingleTableWhere composeCondition) {
        where.addComposeCondition(composeCondition);
        return this;
    }

    public QueryData<T> addGroupBy(FunctionSerialize<T,?> groupBy) {
        groupByList.add(groupBy);
        return this;
    }

    public QueryData<T> addHaving(String alias, ConditionType type, Object value) {
        havingSet.add(alias + type.getValue());
        return this;
    }

    public QueryData<T> addOrder(FunctionSerialize<T,?> column, QueryOrder order) {
        orderMap.put(column, order);
        return this;
    }

    public String generateSql(TableColumnInfo tcInfo) {
        Table table = tcInfo.findTableByClass(clazz);
        if (QueryUtil.isNull(table)) {
            throw new RuntimeException("no ({" + clazz + "}) table has defined");
        }
        return null;
    }

    public static <T> QueryData<T> create(Class<T> clazz) {
        return new QueryData<>(clazz);
    }
}
