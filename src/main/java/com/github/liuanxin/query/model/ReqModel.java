package com.github.liuanxin.query.model;

import com.github.liuanxin.query.enums.ResultType;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class ReqModel implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主表 */
    private String table;
    /** 出参 */
    private ReqResult result;
    /** 出参类型(用在非分页查询), 对象(obj)还是数组(arr), 如果是对象则会在查询上拼 LIMIT 1 条件, 不设置则是数组 */
    private ResultType type;

    private List<List<String>> relationList;

    public ReqModel() {}
    public ReqModel(String table) {
        this.table = table;
    }
    public ReqModel(String table, ReqResult result) {
        this.table = table;
        this.result = result;
    }
    public ReqModel(String table, ReqResult result, ResultType type) {
        this.table = table;
        this.result = result;
        this.type = type;
    }
    public ReqModel(String table, ReqResult result, ResultType type, List<List<String>> relationList) {
        this.table = table;
        this.result = result;
        this.type = type;
        this.relationList = relationList;
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

    public List<List<String>> getRelationList() {
        return relationList;
    }
    public void setRelationList(List<List<String>> relationList) {
        this.relationList = relationList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReqModel)) return false;
        ReqModel that = (ReqModel) o;
        return Objects.equals(table, that.table) && Objects.equals(result, that.result)
                && type == that.type && Objects.equals(relationList, that.relationList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, result, type);
    }

    @Override
    public String toString() {
        return "ReqModel{" +
                "table='" + table + '\'' +
                ", result=" + result +
                ", type=" + type +
                ", relationList=" + relationList +
                '}';
    }
}
