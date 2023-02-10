package com.github.liuanxin.query.model;

import com.github.liuanxin.query.enums.ResultType;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class ReqAlias implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主表 */
    private String table;
    /** 出参 */
    private ReqResult result;
    /** 出参类型(用在非分页查询), 对象(obj)还是数组(arr), 如果是对象则会在查询上拼 LIMIT 1 条件, 不设置则是数组 */
    private ResultType type;

    /** 别名中入参里面设定不发起 SELECT COUNT(*) 查询. 别名中设定了此值将会覆盖入参上的值 */
    private Boolean notCount;
    /** 别名中入参里用到的表的关系. 如: [ [ "order", "inner", "orderAddress" ] , [ "order", "left", "orderItem" ] ]. 别名中设定了此值将会覆盖入参上的值 */
    private List<List<String>> relationList;

    public ReqAlias() {}
    public ReqAlias(String table) {
        this.table = table;
    }
    public ReqAlias(String table, ReqResult result) {
        this.table = table;
        this.result = result;
    }
    public ReqAlias(String table, ReqResult result, List<List<String>> relationList) {
        this.table = table;
        this.result = result;
        this.relationList = relationList;
    }
    public ReqAlias(String table, ReqResult result, ResultType type) {
        this.table = table;
        this.result = result;
        this.type = type;
    }
    public ReqAlias(String table, ReqResult result, ResultType type, Boolean notCount, List<List<String>> relationList) {
        this.table = table;
        this.result = result;
        this.type = type;
        this.notCount = notCount;
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

    public Boolean getNotCount() {
        return notCount;
    }
    public void setNotCount(Boolean notCount) {
        this.notCount = notCount;
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
        if (!(o instanceof ReqAlias)) return false;
        ReqAlias that = (ReqAlias) o;
        return Objects.equals(table, that.table) && Objects.equals(result, that.result) && type == that.type
                && Objects.equals(notCount, that.notCount) && Objects.equals(relationList, that.relationList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, result, type, notCount, relationList);
    }

    @Override
    public String toString() {
        return "ReqAlias{" +
                "table='" + table + '\'' +
                ", result=" + result +
                ", type=" + type +
                ", notCount=" + notCount +
                ", relationList=" + relationList +
                '}';
    }
}
