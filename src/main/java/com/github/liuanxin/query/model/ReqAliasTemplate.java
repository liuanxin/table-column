package com.github.liuanxin.query.model;

import com.github.liuanxin.query.enums.ResultType;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/** 别名模板 */
public class ReqAliasTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主表 */
    private String table;

    /** 别名里的查询部分 */
    private ReqAliasTemplateQuery query;

    /** 别名中入参里面设定不发起 SELECT COUNT(*) 查询. 别名中设定了此值将会覆盖入参上的值 */
    private Boolean notCount;

    /** 别名中入参里用到的表的关系. 如: [ [ "order", "inner", "orderAddress" ] , [ "order", "left", "orderItem" ] ]. 别名中设定了此值将会覆盖入参上的值 */
    private List<List<String>> relationList;

    /** 出参 */
    private ReqResult result;

    /** 出参类型(用在非分页查询), 对象(obj)还是数组(arr), 如果是对象则会在查询上拼 LIMIT 1 条件, 不设置则是数组 */
    private ResultType type;


    public ReqAliasTemplate() {}
    public ReqAliasTemplate(String table) {
        this.table = table;
    }
    public ReqAliasTemplate(String table, ReqAliasTemplateQuery query, List<List<String>> relationList, ReqResult result) {
        this.table = table;
        this.query = query;
        this.relationList = relationList;
        this.result = result;
    }
    public ReqAliasTemplate(String table, ReqAliasTemplateQuery query, List<List<String>> relationList, ReqResult result, ResultType type) {
        this.table = table;
        this.query = query;
        this.relationList = relationList;
        this.result = result;
        this.type = type;
    }
    public ReqAliasTemplate(String table, ReqAliasTemplateQuery query, Boolean notCount,
                            List<List<String>> relationList, ReqResult result, ResultType type) {
        this.table = table;
        this.query = query;
        this.notCount = notCount;
        this.relationList = relationList;
        this.result = result;
        this.type = type;
    }


    public String getTable() {
        return table;
    }
    public void setTable(String table) {
        this.table = table;
    }

    public ReqAliasTemplateQuery getQuery() {
        return query;
    }
    public void setQuery(ReqAliasTemplateQuery query) {
        this.query = query;
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
        ReqAliasTemplate reqAliasTemplate = (ReqAliasTemplate) o;
        return Objects.equals(table, reqAliasTemplate.table) && Objects.equals(query, reqAliasTemplate.query)
                && Objects.equals(notCount, reqAliasTemplate.notCount) && Objects.equals(relationList, reqAliasTemplate.relationList)
                && Objects.equals(result, reqAliasTemplate.result) && type == reqAliasTemplate.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, query, notCount, relationList, result, type);
    }

    @Override
    public String toString() {
        return "ReqAlias{" +
                "table='" + table + '\'' +
                ", query=" + query +
                ", notCount=" + notCount +
                ", relationList=" + relationList +
                ", result=" + result +
                ", type=" + type +
                '}';
    }
}
