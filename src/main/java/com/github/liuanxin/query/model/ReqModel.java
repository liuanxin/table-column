package com.github.liuanxin.query.model;

import com.github.liuanxin.query.enums.ResultType;
import com.github.liuanxin.query.util.QueryUtil;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ReqModel {

    /** 主表 */
    private String table;
    /** 出参 */
    private ReqResult result;
    /** 出参类型(用在非分页查询), 对象(obj)还是数组(arr), 如果是对象则会在查询上拼 LIMIT 1 条件, 不设置则是数组 */
    private ResultType type;
    /** 入参里用到的表的关系. 如: [ [ "order", "inner", "orderAddress" ] , [ "order", "left", "orderItem" ] , [ "order", "right", "orderLog" ] ] */
    private List<List<String>> relation;

    public ReqModel() {}
    public ReqModel(String table, ReqResult result, ResultType type, List<List<String>> relation) {
        this.table = table;
        this.result = result;
        this.type = type;
        this.relation = relation;
    }

    public String getTable() {
        return table;
    }
    public void setTable(String table) {
        this.table = table;
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

    public List<List<String>> getRelation() {
        return relation;
    }
    public void setRelation(List<List<String>> relation) {
        this.relation = relation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReqModel)) return false;
        ReqModel that = (ReqModel) o;
        return Objects.equals(table, that.table) && Objects.equals(result, that.result)
                && type == that.type && Objects.equals(relation, that.relation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, result, type, relation);
    }

    @Override
    public String toString() {
        return "ReqModel{" +
                "table='" + table + '\'' +
                ", result=" + result +
                ", type=" + type +
                ", relation=" + relation +
                '}';
    }


    protected void fillAlias(String alias, Map<String, ReqModel> requestAliasMap) {
        ReqModel model = requestAliasMap.get(alias);
        if (QueryUtil.isNotNull(model)) {
            this.table = model.getTable();
            this.result = model.getResult();
            this.type = model.getType();
            this.relation = model.getRelation();
        }
    }
}
