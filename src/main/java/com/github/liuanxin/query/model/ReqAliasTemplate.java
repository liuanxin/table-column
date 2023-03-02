package com.github.liuanxin.query.model;

import com.github.liuanxin.query.enums.ResultType;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 别名模板 */
public class ReqAliasTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主表 */
    private String table;

    /** 别名里的查询部分 */
    private ReqAliasTemplateQuery query;

    /** 别名里的默认排序信息. 如: { "字段": "asc", "关联表.字段", "desc" } */
    private Map<String, String> sort;

    /** 别名里的默认分页信息 [ 当前页, 每页行数 ]. 如: [ 1, 10 ] 表示查询第 1 页且查 10 条 */
    private List<String> page;

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
    public ReqAliasTemplate(String table, ReqAliasTemplateQuery query, Map<String, String> sort, List<String> page,
                            List<List<String>> relationList, ReqResult result, ResultType type) {
        this.table = table;
        this.query = query;
        this.sort = sort;
        this.page = page;
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

    public Map<String, String> getSort() {
        return sort;
    }
    public void setSort(Map<String, String> sort) {
        this.sort = sort;
    }

    public List<String> getPage() {
        return page;
    }
    public void setPage(List<String> page) {
        this.page = page;
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
        ReqAliasTemplate that = (ReqAliasTemplate) o;
        return Objects.equals(table, that.table) && Objects.equals(query, that.query)
                && Objects.equals(sort, that.sort) && Objects.equals(page, that.page)
                && Objects.equals(notCount, that.notCount) && Objects.equals(relationList, that.relationList)
                && Objects.equals(result, that.result) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, query, sort, page, notCount, relationList, result, type);
    }

    @Override
    public String toString() {
        return "ReqAliasTemplate{" +
                "table='" + table + '\'' +
                ", query=" + query +
                ", sort=" + sort +
                ", page=" + page +
                ", notCount=" + notCount +
                ", relationList=" + relationList +
                ", result=" + result +
                ", type=" + type +
                '}';
    }
}
