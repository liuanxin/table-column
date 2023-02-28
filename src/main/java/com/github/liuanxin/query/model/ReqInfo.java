package com.github.liuanxin.query.model;

import com.github.liuanxin.query.enums.JoinType;
import com.github.liuanxin.query.enums.ResultType;
import com.github.liuanxin.query.util.QueryUtil;

import java.io.Serializable;
import java.util.*;

public class ReqInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 查询别名(如果使用, 下面的 table param result type 将不再生效) */
    private String alias;
    /** 使用别名时的查询条件 */
    private ReqParamAlias aliasQuery;

    // 如果使用别名只需要上面的两个参数, 如果不用别名则使用下面的四个参数

    /** 主表 */
    private String table;
    /** 入参 */
    private ReqParam param;
    /** 出参 */
    private ReqResult result;
    /** 出参类型(用在非分页查询), 对象(obj)还是数组(arr), 如果是对象则会在查询上拼 LIMIT 1 条件, 不设置则是数组 */
    private ResultType type;

    public ReqInfo() {}
    public ReqInfo(ReqParam param, String table, ResultType type) {
        this.table = table;
        this.type = type;
        this.param = param;
    }
    public ReqInfo(ReqParam param, String table) {
        this.table = table;
        this.param = param;
    }
    public ReqInfo(String alias, ReqParam param, String table, ReqResult result, ResultType type) {
        this.alias = alias;
        this.param = param;
        this.table = table;
        this.result = result;
        this.type = type;
    }
    public ReqInfo(String table, ReqResult result, ResultType type, ReqParam param) {
        this.param = param;
        this.table = table;
        this.result = result;
        this.type = type;
    }


    public String getAlias() {
        return alias;
    }
    public void setAlias(String alias) {
        this.alias = alias;
    }

    public ReqParamAlias getAliasQuery() {
        return aliasQuery;
    }
    public void setAliasQuery(ReqParamAlias aliasQuery) {
        this.aliasQuery = aliasQuery;
    }

    public String getTable() {
        return table;
    }
    public void setTable(String table) {
        this.table = table;
    }

    public ReqParam getParam() {
        return param;
    }
    public void setParam(ReqParam param) {
        this.param = param;
    }

    public ReqResult getResult() {
        return result;
    }
    public void setResult(ReqResult result) {
        this.result = result;
    }

    public ResultType getType() {
        return type;
    }
    public void setType(ResultType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReqInfo reqInfo = (ReqInfo) o;
        return Objects.equals(alias, reqInfo.alias) && Objects.equals(aliasQuery, reqInfo.aliasQuery)
                && Objects.equals(table, reqInfo.table) && Objects.equals(param, reqInfo.param)
                && Objects.equals(result, reqInfo.result) && type == reqInfo.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(alias, aliasQuery, table, param, result, type);
    }

    @Override
    public String toString() {
        return "ReqInfo{" +
                "alias='" + alias + '\'' +
                ", aliasQuery=" + aliasQuery +
                ", table='" + table + '\'' +
                ", param=" + param +
                ", result=" + result +
                ", type=" + type +
                '}';
    }


    public void handleAlias(boolean requiredAlias, Map<String, ReqAlias> requestAliasMap) {
        if (requiredAlias && QueryUtil.isEmpty(alias)) {
            throw new RuntimeException("request: required request alias");
        }
        if (QueryUtil.isNotEmpty(alias)) {
            if (QueryUtil.isNotEmpty(table) || QueryUtil.isNotNull(param) || QueryUtil.isNotNull(result) || QueryUtil.isNotNull(type)) {
                throw new RuntimeException("request: if use alias, just need alias + aliasQuery");
            }
            if (QueryUtil.isEmpty(requestAliasMap)) {
                throw new RuntimeException("request: no define request alias");
            }
            ReqAlias aliasParam = requestAliasMap.get(alias);
            if (QueryUtil.isNull(aliasParam)) {
                throw new RuntimeException("request: no request alias(" + alias + ") info");
            }

            String table = aliasParam.getTable();
            if (QueryUtil.isNotEmpty(table)) {
                this.table = table;
            }
            ReqResult result = aliasParam.getResult();
            if (QueryUtil.isNotNull(result)) {
                this.result = result;
            }
            ResultType type = aliasParam.getType();
            if (QueryUtil.isNotNull(type)) {
                this.type = type;
            }


            param = new ReqParam();
            Boolean notCount = aliasParam.getNotCount();
            if (QueryUtil.isNotNull(notCount)) {
                param.setNotCount(notCount);
            }
            List<List<String>> relationList = aliasParam.getRelationList();
            if (QueryUtil.isNotEmpty(relationList)) {
                param.setRelation(relationList);
            }
            if (QueryUtil.isNotNull(aliasQuery)) {
                Map<String, String> sort = aliasQuery.getSort();
                if (QueryUtil.isNotEmpty(sort)) {
                    param.setSort(sort);
                }
                List<String> page = aliasQuery.getPage();
                if (QueryUtil.isNotEmpty(page)) {
                    param.setPage(page);
                }
                Map<String, Object> paramMap = aliasQuery.getQuery();
                ReqAliasQuery aliasQuery = aliasParam.getQuery();
                if (QueryUtil.isNotEmpty(paramMap) && QueryUtil.isNotNull(aliasQuery)) {
                    param.setQuery(handleAliasQuery(paramMap, aliasQuery));
                }
            }
        }
    }

    /**
     * <pre>
     * 模板
     * {
     *   "operate": "and",
     *   "conditions": [
     *     { "name": "$start" },
     *     { "_meta_name_": "startTime", "time": "$ge" },
     *     { "_meta_name_": "endTime", "time": "$le" },
     *     {
     *       "operate": "or",
     *       "name": "x",
     *       "conditions": [
     *         { "gender": "$eq" },
     *         { "age": "$bet" }
     *       ]
     *     },
     *     {
     *       "operate": "or",
     *       "name": "y",
     *       "conditions": [
     *         { "province": "$in" },
     *         { "city": "$fuzzy" }
     *       ]
     *     },
     *     { "status": "$eq" }
     *   ]
     * }
     *
     * 数据
     * {
     *   "name": "abc",
     *   "startTime": "xxxx-xx-xx xx:xx:xx",
     *   "endTime": "yyyy-yy-yy yy:yy:yy",
     *   "x": { "gender": 1, "age": [ 18, 40 ] },
     *   "y": { "province": [ "x", "y", "z" ], "city": "xx" },
     *   "status": 1
     * }
     *
     *
     * 最终生成
     * {
     *   "operate": "and",
     *   "conditions": [
     *     [ "name", "$start", "abc" ],
     *     [ "time", "$ge", "xxxx-xx-xx xx:xx:xx" ],
     *     [ "time", "$le", "yyyy-yy-yy yy:yy:yy" ],
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
     *     [ "status", "$ge", 1 ]
     *   ]
     * }
     *
     * 其生成的查询是
     * name like 'abc%'
     * and time >= 'xxxx-xx-xx xx:xx:xx'
     * and time <= 'yyyy-yy-yy yy:yy:yy'
     * and ( gender = 1 or age between 18 and 40 )
     * and ( province in ( 'x', 'y', 'z' ) or city like '%xx%' )
     * and status = 1
     * </pre>
     */
    private ReqQuery handleAliasQuery(Map<String, Object> paramMap, ReqAliasQuery aliasQuery) {
        ReqQuery query = new ReqQuery();
        for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
        }
        // todo
        return query;
    }


    public void checkTable(TableColumnInfo tcInfo) {
        String table = getTable();
        if (QueryUtil.isEmpty(table)) {
            throw new RuntimeException("request: need table");
        }
        if (QueryUtil.isNull(tcInfo.findTableWithAlias(table))) {
            throw new RuntimeException("request: table(" + table + ") has no defined");
        }
    }

    public Set<String> checkParam(boolean notRequiredConditionOrPage, TableColumnInfo tcInfo, int maxListCount) {
        if (QueryUtil.isNull(param)) {
            throw new RuntimeException("request: need param");
        }
        return param.checkParam(notRequiredConditionOrPage, getTable(), tcInfo, maxListCount);
    }


    public void checkResult(TableColumnInfo tcInfo, boolean force) {
        ReqResult result = getResult();
        if (QueryUtil.isNull(result)) {
            result = new ReqResult();
            setResult(result);
        }
        result.checkResult(getTable(), tcInfo, force);
    }

    public Set<TableJoinRelation> checkRelation(TableColumnInfo tcInfo, Set<String> paramTableSet) {
        List<List<String>> relation = param.getRelation();
        Map<String, Set<TableJoinRelation>> relationMap = new HashMap<>();
        if (QueryUtil.isNotEmpty(relation)) {
            for (List<String> values : relation) {
                if (values.size() < 3) {
                    throw new RuntimeException("relation error, for example: [ table1, left, table2 ]");
                }
                Table masterTable = tcInfo.findTable(values.get(0));
                Table childTable = tcInfo.findTable(values.get(2));
                String mn = masterTable.getName();
                String cn = childTable.getName();
                if (paramTableSet.contains(mn) && paramTableSet.contains(cn)) {
                    JoinType joinType = JoinType.deserializer(values.get(1));
                    TableJoinRelation joinRelation = new TableJoinRelation(masterTable, joinType, childTable);
                    relationMap.computeIfAbsent(masterTable.getName(), k -> new LinkedHashSet<>()).add(joinRelation);
                }
            }
        }
        return handleRelation(tcInfo.findTable(getTable()).getName(), relationMap);
    }

    public void checkAllTable(TableColumnInfo tcInfo, Set<String> allTableSet, Set<String> paramTableSet) {
        Table tableInfo = tcInfo.findTable(getTable());
        paramTableSet.remove(tableInfo.getName());
        if (QueryUtil.isEmpty(param.getRelation())) {
            if (QueryUtil.isNotEmpty(paramTableSet)) {
                throw new RuntimeException("request: need relation");
            }
        }
        checkRelation(tcInfo);

        for (String paramTable : paramTableSet) {
            if (!allTableSet.contains(paramTable)) {
                throw new RuntimeException("relation: need param table(" + tcInfo.findTable(paramTable).getAlias() + ")");
            }
        }
    }
    private void checkRelation(TableColumnInfo tcInfo) {
        List<List<String>> relation = param.getRelation();
        if (QueryUtil.isNotEmpty(relation)) {
            String append = "<->";
            Set<String> tableRelation = new HashSet<>();
            for (List<String> values : relation) {
                JoinType joinType = JoinType.deserializer(values.get(1));
                if (QueryUtil.isNull(joinType)) {
                    throw new RuntimeException("relation join type error, support: inner left right");
                }
                String masterTable = values.get(0);
                String childTable = values.get(2);
                if (QueryUtil.isNull(tcInfo.findRelationByMasterChildWithAlias(masterTable, childTable))) {
                    throw new RuntimeException("relation: " + masterTable + " - " + childTable + " has no relation");
                }

                String key = masterTable + append + childTable;
                if (tableRelation.contains(key)) {
                    throw new RuntimeException("relation: " + masterTable + " - " + childTable + " only can be one relation");
                }
                tableRelation.add(key);
            }
            String table = getTable();
            boolean hasMain = false;
            for (String tr : tableRelation) {
                if (tr.startsWith(table + append)) {
                    hasMain = true;
                    break;
                }
            }
            if (!hasMain) {
                throw new RuntimeException("relation: has no " + table + "'s info");
            }
        }
    }

    private Set<TableJoinRelation> handleRelation(String mainTable, Map<String, Set<TableJoinRelation>> relationMap) {
        Set<TableJoinRelation> relationSet = new LinkedHashSet<>();
        Set<String> tableSet = new HashSet<>();
        Set<TableJoinRelation> mainSet = relationMap.remove(mainTable);
        if (QueryUtil.isNotEmpty(mainSet)) {
            for (TableJoinRelation relation : mainSet) {
                relationSet.add(relation);
                tableSet.add(relation.getMasterTable().getName());
                tableSet.add(relation.getChildTable().getName());
            }
        }
        for (int i = 0; i < relationMap.size(); i++) {
            for (Map.Entry<String, Set<TableJoinRelation>> entry : relationMap.entrySet()) {
                if (tableSet.contains(entry.getKey())) {
                    for (TableJoinRelation relation : entry.getValue()) {
                        relationSet.add(relation);
                        tableSet.add(relation.getMasterTable().getName());
                        tableSet.add(relation.getChildTable().getName());
                    }
                }
            }
        }
        return relationSet;
    }
}
