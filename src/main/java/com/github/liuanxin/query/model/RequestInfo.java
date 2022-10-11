package com.github.liuanxin.query.model;

import com.github.liuanxin.query.enums.JoinType;
import com.github.liuanxin.query.enums.ResultType;

import java.util.*;

public class RequestInfo {

    /** 主表 */
    private String table;
    /** 入参 */
    private ReqParam param;
    /** 出参类型, 对象(obj)还是数组(arr), 不设置则是数组 */
    private ResultType type;
    /** 出参 */
    private ReqResult result;

    /** { [ "order", "inner", "orderAddress" ] , [ "order", "left", "orderItem" ] , [ "order", "right", "orderPrice" ] ] */
    private List<List<String>> relation;

    public RequestInfo() {}
    public RequestInfo(String table, ReqParam param, ResultType type, ReqResult result, List<List<String>> relation) {
        this.table = table;
        this.param = param;
        this.type = type;
        this.result = result;
        this.relation = relation;
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

    public List<List<String>> getRelation() {
        return relation;
    }
    public void setRelation(List<List<String>> relation) {
        this.relation = relation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RequestInfo)) return false;
        RequestInfo that = (RequestInfo) o;
        return Objects.equals(table, that.table) && Objects.equals(param, that.param)
                && type == that.type && Objects.equals(result, that.result)
                && Objects.equals(relation, that.relation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, param, type, result, relation);
    }

    @Override
    public String toString() {
        return "RequestInfo{" +
                "table='" + table + '\'' +
                ", param=" + param +
                ", type=" + type +
                ", result=" + result +
                ", relation=" + relation +
                '}';
    }


    public void checkTable(TableColumnInfo tcInfo) {
        if (table == null || table.isEmpty()) {
            throw new RuntimeException("request need table");
        }
        if (tcInfo.findTable(table) == null) {
            throw new RuntimeException("request has no defined table(" + table + ")");
        }
    }

    public Set<String> checkParam(TableColumnInfo tcInfo) {
        if (param == null) {
            throw new RuntimeException("request need param");
        }
        return param.checkParam(table, tcInfo);
    }

    public Set<String> checkResult(TableColumnInfo tcInfo, Set<String> allResultTable) {
        if (result == null) {
            throw new RuntimeException("request need result");
        }
        return result.checkResult(table, tcInfo, allResultTable);
    }

    public void checkAllTable(TableColumnInfo tcInfo, Set<String> allTableSet,
                               Set<String> paramTableSet, Set<String> resultTableSet) {
        paramTableSet.remove(table);
        resultTableSet.remove(table);
        if (relation == null || relation.isEmpty()) {
            if (!paramTableSet.isEmpty() || !resultTableSet.isEmpty()) {
                throw new RuntimeException("request need relation");
            }
        }
        checkRelation(tcInfo);

        for (String paramTable : paramTableSet) {
            if (!allTableSet.contains(paramTable)) {
                throw new RuntimeException("relation need param table(" + paramTable + ")");
            }
        }
        for (String resultTable : resultTableSet) {
            if (!allTableSet.contains(resultTable)) {
                throw new RuntimeException("relation need result table(" + resultTable + ")");
            }
        }
    }
    private void checkRelation(TableColumnInfo tcInfo) {
        if (relation != null && !relation.isEmpty()) {
            Set<String> tableRelation = new HashSet<>();
            for (List<String> values : relation) {
                if (values.size() < 3) {
                    throw new RuntimeException("relation error");
                }
                JoinType joinType = JoinType.deserializer(values.get(1));
                if (joinType == null) {
                    throw new RuntimeException("relation join type error");
                }
                String masterTable = values.get(0);
                String childTable = values.get(2);
                if (tcInfo.findRelationByMasterChild(masterTable, childTable) == null) {
                    throw new RuntimeException("relation " + masterTable + " and " + childTable + " has no relation");
                }

                String key = masterTable + "<->" + childTable;
                if (tableRelation.contains(key)) {
                    throw new RuntimeException("relation " + masterTable + " and " + childTable + " can only has one relation");
                }
                tableRelation.add(key);
            }
            boolean hasMain = false;
            for (String table : tableRelation) {
                if (table.startsWith(table + ".")) {
                    hasMain = true;
                    break;
                }
            }
            if (!hasMain) {
                throw new RuntimeException("relation has no " + table + "'s info");
            }
        }
    }

    public List<TableJoinRelation> allRelationList(TableColumnInfo tcInfo) {
        Map<String, Set<TableJoinRelation>> relationMap = new HashMap<>();
        if (relation != null && !relation.isEmpty()) {
            for (List<String> values : relation) {
                Table masterTable = tcInfo.findTable(values.get(0));
                Table childTable = tcInfo.findTable(values.get(2));
                JoinType joinType = JoinType.deserializer(values.get(1));
                TableJoinRelation joinRelation = new TableJoinRelation(masterTable, joinType, childTable);
                Set<TableJoinRelation> relationSet = relationMap.getOrDefault(masterTable.getName(), new LinkedHashSet<>());
                relationSet.add(joinRelation);
                relationMap.put(masterTable.getName(), relationSet);
            }
        }
        return handleRelation(table, relationMap);
    }
    private List<TableJoinRelation> handleRelation(String mainTable, Map<String, Set<TableJoinRelation>> relationMap) {
        Set<TableJoinRelation> relationSet = new LinkedHashSet<>();
        Set<String> tableSet = new HashSet<>();
        Set<TableJoinRelation> mainSet = relationMap.remove(mainTable);
        if (mainSet != null && !mainSet.isEmpty()) {
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
        return new ArrayList<>(relationSet);
    }
    public List<TableJoinRelation> paramRelationList(TableColumnInfo tcInfo, Set<String> paramTableSet,
                                                     Set<String> resultFunctionTableSet) {
        Map<String, Set<TableJoinRelation>> relationMap = new HashMap<>();
        if (relation != null && !relation.isEmpty()) {
            for (List<String> values : relation) {
                Table masterTable = tcInfo.findTable(values.get(0));
                Table childTable = tcInfo.findTable(values.get(2));
                String mn = masterTable.getName();
                String cn = childTable.getName();
                if ((paramTableSet.contains(mn) && paramTableSet.contains(cn))
                        || (resultFunctionTableSet.contains(mn) && resultFunctionTableSet.contains(cn))) {
                    Set<TableJoinRelation> relationSet = relationMap.getOrDefault(masterTable.getName(), new LinkedHashSet<>());
                    JoinType joinType = JoinType.deserializer(values.get(1));
                    TableJoinRelation joinRelation = new TableJoinRelation(masterTable, joinType, childTable);
                    relationSet.add(joinRelation);
                    relationMap.put(masterTable.getName(), relationSet);
                }
            }
        }
        return handleRelation(table, relationMap);
    }
}
