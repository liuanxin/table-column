package com.github.liuanxin.query.model;

import com.github.liuanxin.query.enums.ResultType;
import com.github.liuanxin.query.util.QueryUtil;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RequestModel {

    /** 主表 */
    private String table;
    /** 出参类型, 对象(obj)还是数组(arr), 不设置则是数组 */
    private ResultType type;
    /** 出参 */
    private ReqResult result;
    /** [ [ "order", "inner", "orderAddress" ] , [ "order", "left", "orderItem" ] , [ "order", "right", "orderPrice" ] ] */
    private List<List<String>> relation;

    public RequestModel() {}
    public RequestModel(String table, ResultType type, ReqResult result, List<List<String>> relation) {
        this.table = table;
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
        if (!(o instanceof RequestModel)) return false;
        RequestModel that = (RequestModel) o;
        return Objects.equals(table, that.table) && type == that.type
                && Objects.equals(result, that.result) && Objects.equals(relation, that.relation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, type, result, relation);
    }

    @Override
    public String toString() {
        return "RequestInfo{" +
                "table='" + table + '\'' +
                ", type=" + type +
                ", result=" + result +
                ", relation=" + relation +
                '}';
    }


    protected void fillAlias(String alias, Map<String, RequestModel> requestAliasMap) {
        if (QueryUtil.isNotEmpty(alias) && QueryUtil.isNotEmpty(requestAliasMap)) {
            RequestModel model = requestAliasMap.get(alias);
            if (QueryUtil.isNotNull(model)) {
                this.table = model.getTable();
                this.type = model.getType();
                this.result = model.getResult();
                this.relation = model.getRelation();
            }
        }
    }
}
