package com.github.liuanxin.query.model;

import com.github.liuanxin.query.enums.JoinType;
import com.github.liuanxin.query.enums.ResultType;
import com.github.liuanxin.query.util.QueryUtil;

import java.io.Serializable;
import java.util.*;

public class ReqInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主表 */
    private String table;
    /** 入参 */
    private ReqParam param;
    /** 出参类型(用在非分页查询), 对象(obj)还是数组(arr), 如果是对象则会在查询上拼 LIMIT 1 条件, 不设置则是数组 */
    private ResultType type;
    /** 出参 */
    private ReqResult result;


    public ReqInfo() {}
    public ReqInfo(String table, ReqParam param, ResultType type, ReqResult result) {
        this.table = table;
        this.param = param;
        this.type = type;
        this.result = result;
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

    public ResultType getType() {
        return type;
    }
    public void setType(ResultType type) {
        this.type = type;
    }

    public ReqResult getResult() {
        return result;
    }
    public void setResult(ReqResult result) {
        this.result = result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReqInfo reqInfo = (ReqInfo) o;
        return Objects.equals(table, reqInfo.table) && Objects.equals(param, reqInfo.param)
                && type == reqInfo.type && Objects.equals(result, reqInfo.result);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, param, type, result);
    }

    @Override
    public String toString() {
        return "ReqInfo{" +
                ", table='" + table + '\'' +
                ", param=" + param +
                ", type=" + type +
                ", result=" + result +
                '}';
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
