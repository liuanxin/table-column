package com.github.liuanxin.query.model;

import com.github.liuanxin.query.constant.QueryConst;
import com.github.liuanxin.query.enums.*;
import com.github.liuanxin.query.function.FunctionSerialize;
import com.github.liuanxin.query.util.QueryLambdaUtil;
import com.github.liuanxin.query.util.QueryUtil;

import java.io.Serializable;
import java.util.*;

@SuppressWarnings("DuplicatedCode")
public class QueryData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String table;
    private Class<?> clazz;
    private final Set<QueryRelation> relationSet;
    private final Set<String> selectSet;
    private final Map<String, List<String>> functionMap;
    private final ReqQuery query;
    private final Set<String> groupBySet;
    private final Map<String, List<Object>> havingMap;
    private final Map<String, String> sortMap;
    private final List<Integer> pageList;

    private QueryData() {
        relationSet = new LinkedHashSet<>();
        selectSet = new LinkedHashSet<>();
        functionMap = new LinkedHashMap<>();
        query = new ReqQuery();
        groupBySet = new LinkedHashSet<>();
        havingMap = new HashMap<>();
        sortMap = new LinkedHashMap<>();
        pageList = new ArrayList<>();
    }

    public void clear() {
        relationSet.clear();
        selectSet.clear();
        functionMap.clear();
        query.clear();
        groupBySet.clear();
        havingMap.clear();
        sortMap.clear();
        pageList.clear();
    }

    public QueryData setTable(String table) {
        this.table = table;
        return this;
    }

    public QueryData setClazz(Class<?> clazz) {
        this.clazz = clazz;
        return this;
    }

    public QueryData addSelect(FunctionSerialize<?, ?> selectColumn, FunctionSerialize<?, ?>... selectColumns) {
        if (QueryUtil.isNotNull(selectColumn)) {
            String select = QueryLambdaUtil.toTableAndColumn(selectColumn);
            if (QueryUtil.isNotEmpty(select)) {
                selectSet.add(select);
            }
        }
        if (selectColumns != null) {
            for (FunctionSerialize<?, ?> sc : selectColumns) {
                String select = QueryLambdaUtil.toTableAndColumn(sc);
                if (QueryUtil.isNotEmpty(select)) {
                    selectSet.add(select);
                }
            }
        }
        return this;
    }
    public QueryData addSelect(String selectColumn, String... selectColumns) {
        if (QueryUtil.isNotEmpty(selectColumn)) {
            selectSet.add(selectColumn);
        }
        if (selectColumns != null) {
            for (String sc : selectColumns) {
                if (QueryUtil.isNotEmpty(sc)) {
                    selectSet.add(sc);
                }
            }
        }
        return this;
    }

    public QueryData addFunction(String alias, ResultGroup func, FunctionSerialize<?, ?>... functionColumns) {
        if (QueryUtil.isNotEmpty(alias) && QueryUtil.isNotNull(func) && functionColumns != null) {
            if (func == ResultGroup.COUNT) {
                // [ "abc", "count", "*" ]
                functionMap.put(alias, Arrays.asList(func.value(), "*"));
            } else {
                List<String> funList = new ArrayList<>();
                for (FunctionSerialize<?, ?> column : functionColumns) {
                    String tc = QueryLambdaUtil.toTableAndColumn(column);
                    if (QueryUtil.isNotEmpty(tc)) {
                        funList.add(tc);
                    }
                }
                if (QueryUtil.isNotEmpty(funList)) {
                    // [ "def", "count_distinct", "name, name2" ]
                    functionMap.put(alias, Arrays.asList(func.value(), func.generateColumn(String.join(", ", funList))));
                }
            }
        }
        return this;
    }
    public QueryData addFunction(String alias, ResultGroup func, String... functionColumns) {
        if (QueryUtil.isNotEmpty(alias) && QueryUtil.isNotNull(func) && functionColumns != null) {
            if (func == ResultGroup.COUNT) {
                // [ "abc", "count", "*" ]
                functionMap.put(alias, Arrays.asList(func.value(), "*"));
            } else {
                // [ "def", "count_distinct", "name, name2" ]
                functionMap.put(alias, Arrays.asList(func.value(), func.generateColumn(String.join(", ", functionColumns))));
            }
        }
        return this;
    }

    public QueryData addJoin(Class<?> left, JoinType joinType, Class<?> right) {
        if (QueryUtil.isNotNull(left) && QueryUtil.isNotNull(joinType) && QueryUtil.isNotNull(right)) {
            relationSet.add(new QueryRelation(left, joinType, right));
        }
        return this;
    }

    public QueryData addCondition(FunctionSerialize<?, ?> column, ConditionType type, Object value) {
        if (QueryUtil.isNotNull(column) && QueryUtil.isNotNull(type)) {
            query.addCondition(column, type, value);
        }
        return this;
    }
    public QueryData addCondition(String column, ConditionType type, Object value) {
        if (QueryUtil.isNotNull(column) && QueryUtil.isNotNull(type)) {
            query.addCondition(column, type, value);
        }
        return this;
    }

    public QueryData addComposeCondition(ReqQuery composeCondition) {
        if (QueryUtil.isNotNull(composeCondition)) {
            query.addComposeCondition(composeCondition);
        }
        return this;
    }

    public QueryData addGroupBy(FunctionSerialize<?, ?> groupBy, FunctionSerialize<?, ?>... groupBys) {
        if (QueryUtil.isNotNull(groupBy)) {
            String group = QueryLambdaUtil.toTableAndColumn(groupBy);
            if (QueryUtil.isNotEmpty(group)) {
                groupBySet.add(group);
            }
        }
        if (groupBys != null) {
            for (FunctionSerialize<?, ?> gb : groupBys) {
                String group = QueryLambdaUtil.toTableAndColumn(gb);
                if (QueryUtil.isNotEmpty(group)) {
                    groupBySet.add(group);
                }
            }
        }
        return this;
    }
    public QueryData addGroupBy(String groupBy, String... groupBys) {
        if (QueryUtil.isNotEmpty(groupBy)) {
            groupBySet.add(groupBy);
        }
        if (groupBys != null) {
            for (String gb : groupBys) {
                if (QueryUtil.isNotEmpty(gb)) {
                    groupBySet.add(gb);
                }
            }
        }
        return this;
    }

    public QueryData addHaving(String alias, ConditionType type, Object value) {
        // [ "ghi", /* "sum", "price", */ "gt", 100.5, "lt", 120.5 ]
        havingMap.computeIfAbsent(alias, k -> new ArrayList<>()).addAll(Arrays.asList(type.value(), value));
        return this;
    }

    public QueryData addOrder(QueryOrder order, FunctionSerialize<?, ?> column, FunctionSerialize<?, ?>... columns) {
        if (QueryUtil.isNotNull(order)) {
            if (QueryUtil.isNotNull(column)) {
                String tc = QueryLambdaUtil.toTableAndColumn(column);
                if (QueryUtil.isNotEmpty(tc)) {
                    sortMap.put(tc, order.value());
                }
            }
            if (columns != null) {
                for (FunctionSerialize<?, ?> col : columns) {
                    String tc = QueryLambdaUtil.toTableAndColumn(col);
                    if (QueryUtil.isNotEmpty(tc)) {
                        sortMap.put(tc, order.value());
                    }
                }
            }
        }
        return this;
    }
    public QueryData addOrder(QueryOrder order, String column, String... columns) {
        if (QueryUtil.isNotNull(order)) {
            if (QueryUtil.isNotNull(column)) {
                sortMap.put(column, order.value());
            }
            if (columns != null) {
                for (String col : columns) {
                    if (QueryUtil.isNotEmpty(col)) {
                        sortMap.put(col, order.value());
                    }
                }
            }
        }
        return this;
    }

    public QueryData withOne() {
        return withPage(1, 1);
    }
    public QueryData withLimit(int limit) {
        return withPage(1, limit);
    }
    public QueryData withPage(int page, int limit) {
        if (page > 0 && limit > 0) {
            pageList.clear();
            pageList.add(page);
            pageList.add(limit);
        }
        return this;
    }

    private String toTableAlias(TableColumnInfo tcInfo) {
        if (QueryUtil.isNull(clazz) && QueryUtil.isEmpty(table)) {
            throw new RuntimeException("need table info");
        }
        Table tb = QueryUtil.isEmpty(table) ? tcInfo.findTableByClass(clazz) : tcInfo.findTable(table);
        if (QueryUtil.isNull(tb)) {
            throw new RuntimeException(String.format("no table(%s) has defined", QueryUtil.isEmpty(table) ? clazz : table));
        }
        return tb.getAlias();
    }
    private ReqResult toResult() {
        Set<Object> columns = new LinkedHashSet<>();
        if (QueryUtil.isNotEmpty(selectSet)) {
            columns.addAll(selectSet);
        }
        if (QueryUtil.isNotEmpty(groupBySet)) {
            columns.addAll(groupBySet);
        }
        if (QueryUtil.isNotEmpty(functionMap)) {
            for (Map.Entry<String, List<String>> entry : functionMap.entrySet()) {
                String alias = entry.getKey();

                List<Object> functionList = new ArrayList<>();
                functionList.add(alias);
                functionList.addAll(entry.getValue());
                functionList.addAll(havingMap.get(alias));
                columns.add(functionList);
            }
        }
        return new ReqResult(new ArrayList<>(columns));
    }
    private ReqResult toCountResult() {
        return new ReqResult(Arrays.asList(QueryConst.COUNT_ALIAS, "count", "*"));
    }
    private List<List<String>> toRelation(TableColumnInfo tcInfo) {
        if (QueryUtil.isNotEmpty(relationSet)) {
            List<List<String>> relationList = new ArrayList<>();
            for (QueryRelation relation : relationSet) {
                Table left = tcInfo.findTableByClass(relation.getLeft());
                if (QueryUtil.isNull(left)) {
                    throw new RuntimeException("table(" + relation.getLeft() + ") has no defined");
                }
                Table right = tcInfo.findTableByClass(relation.getRight());
                if (QueryUtil.isNull(right)) {
                    throw new RuntimeException("table(" + relation.getRight() + ") has no defined");
                }
                TableColumnRelation tcr = tcInfo.findRelationByMasterChild(left.getName(), right.getName());
                if (QueryUtil.isNull(tcr)) {
                    throw new RuntimeException(left.getName() + " - " + right.getName() + " has no relation");
                }
                relationList.add(Arrays.asList(left.getAlias(), relation.getType().value(), right.getAlias()));
            }
            return relationList;
        }
        return null;
    }

    public ReqInfo toQueryCount(TableColumnInfo tcInfo) {
        ReqParam param = new ReqParam(query);
        return new ReqInfo(toTableAlias(tcInfo), toCountResult(), ResultType.OBJ, toRelation(tcInfo), param);
    }

    public ReqInfo toQueryObj(TableColumnInfo tcInfo) {
        ReqParam param = new ReqParam(query, sortMap);
        return new ReqInfo(toTableAlias(tcInfo), toResult(), ResultType.OBJ, toRelation(tcInfo), param);
    }

    public ReqInfo toQueryList(TableColumnInfo tcInfo) {
        ReqParam param = new ReqParam(query, sortMap, pageList, true);
        return new ReqInfo(toTableAlias(tcInfo), toResult(), ResultType.ARR, toRelation(tcInfo), param);
    }

    public ReqInfo toQueryPage(TableColumnInfo tcInfo) {
        ReqParam param = new ReqParam(query, sortMap, pageList);
        return new ReqInfo(toTableAlias(tcInfo), toResult(), ResultType.ARR, toRelation(tcInfo), param);
    }


    public static QueryData create() {
        return new QueryData();
    }
}
