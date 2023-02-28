package com.github.liuanxin.query.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ReqParamAlias implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * <pre>
     * {
     *   "name": "abc",
     *   "startTime": "xxxx-xx-xx xx:xx:xx",
     *   "endTime": "yyyy-yy-yy yy:yy:yy",
     *   "x": { "gender": 1, "age": [ 18, 40 ] },
     *   "y": { "province": [ "x", "y", "z" ], "city": "xx" },
     *   "status": 1
     * }
     * </pre>
     */
    private Map<String, Object> query;
    /** 排序信息. 如: { "字段": "asc", "关联表.字段", "desc" } */
    private Map<String, String> sort;
    /** 分页信息 [ 当前页, 每页行数 ]. 如: [ 1 ] 表示查询第 1 页且查 10 条; [ 2, 20 ] 表示查第 2 页且查 20 条 */
    private List<String> page;


    public Map<String, Object> getQuery() {
        return query;
    }
    public void setQuery(Map<String, Object> query) {
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReqParamAlias that = (ReqParamAlias) o;
        return Objects.equals(query, that.query) && Objects.equals(sort, that.sort) && Objects.equals(page, that.page);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, sort, page);
    }

    @Override
    public String toString() {
        return "ReqAliasParam{" +
                "query=" + query +
                ", sort=" + sort +
                ", page=" + page +
                '}';
    }
}
