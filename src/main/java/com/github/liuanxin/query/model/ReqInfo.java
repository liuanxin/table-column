package com.github.liuanxin.query.model;

import com.github.liuanxin.query.enums.JoinType;
import com.github.liuanxin.query.enums.ResultType;
import com.github.liuanxin.query.util.QueryUtil;

import java.io.Serializable;
import java.util.*;

public class ReqInfo extends ReqModel implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 模板别名, 用这个值映射 RequestModel 的内容 */
    private String alias;
    /** 入参 */
    private ReqParam param;

    public ReqInfo() {}
    public ReqInfo(ReqParam param, String table, ResultType type) {
        super.setTable(table);
        super.setType(type);
        this.param = param;
    }
    public ReqInfo(ReqParam param, String table) {
        super.setTable(table);
        this.param = param;
    }
    public ReqInfo(String alias, ReqParam param, String table, ReqResult result, ResultType type) {
        super(table, result, type);
        this.alias = alias;
        this.param = param;
    }
    public ReqInfo(String table, ReqResult result, ResultType type, ReqParam param) {
        super(table, result, type);
        this.param = param;
    }


    public String getAlias() {
        return alias;
    }
    public void setAlias(String alias) {
        this.alias = alias;
    }

    public ReqParam getParam() {
        return param;
    }
    public void setParam(ReqParam param) {
        this.param = param;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReqInfo)) return false;
        ReqInfo that = (ReqInfo) o;
        return Objects.equals(alias, that.alias) && Objects.equals(param, that.param)
                && Objects.equals(getTable(), that.getTable()) && Objects.equals(getResult(), that.getResult())
                && getType() == that.getType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(alias, param, getTable(), getResult(), getType());
    }

    @Override
    public String toString() {
        return "ReqInfo{" +
                ", alias=" + alias +
                ", param=" + param +
                ", table='" + getTable() + '\'' +
                ", result=" + getResult() +
                ", type=" + getType() +
                '}';
    }


    public void checkAlias(Map<String, ReqModel> requestAliasMap) {
        if (QueryUtil.isEmpty(alias)) {
            throw new RuntimeException("request: need alias");
        }
        if (QueryUtil.isEmpty(requestAliasMap)) {
            throw new RuntimeException("request: no alias has defined");
        }
        ReqModel model = requestAliasMap.get(alias);
        if (QueryUtil.isNull(model)) {
            throw new RuntimeException(String.format("request: no alias(%s) has defined", alias));
        }
    }

    public void handleAlias(boolean requiredAlias, Map<String, ReqModel> requestAliasMap) {
        if (requiredAlias && QueryUtil.isEmpty(alias)) {
            throw new RuntimeException("request: required alias");
        }
        if (QueryUtil.isNotEmpty(alias) && QueryUtil.isNotEmpty(requestAliasMap)) {
            super.fillAlias(alias, requestAliasMap);
        }
    }


    public void checkTable(TableColumnInfo tcInfo) {
        String table = getTable();
        if (QueryUtil.isEmpty(table)) {
            throw new RuntimeException("request: need table");
        }
        if (QueryUtil.isNull(tcInfo.findTableWithAlias(table))) {
            throw new RuntimeException("request: has no defined table(" + table + ")");
        }
    }

    public Set<String> checkParam(boolean needConditionOrPage, TableColumnInfo tcInfo, int maxListCount) {
        if (QueryUtil.isNull(param)) {
            throw new RuntimeException("request: need param");
        }
        return param.checkParam(needConditionOrPage, getTable(), tcInfo, maxListCount);
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
