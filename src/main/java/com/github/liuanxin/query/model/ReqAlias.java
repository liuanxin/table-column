package com.github.liuanxin.query.model;

import com.github.liuanxin.query.util.QueryUtil;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ReqAlias implements Serializable {
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

    public ReqAlias() {}
    public ReqAlias(Map<String, Object> query, Map<String, String> sort, List<String> page) {
        this.query = query;
        this.sort = sort;
        this.page = page;
    }

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
        ReqAlias that = (ReqAlias) o;
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


    public ReqInfo handleAlias(String alias, Map<String, ReqAliasTemplate> requestAliasMap) {
        if (QueryUtil.isEmpty(alias)) {
            throw new RuntimeException("request: required request alias");
        }
        if (QueryUtil.isEmpty(requestAliasMap)) {
            throw new RuntimeException("request: no define alias info");
        }
        ReqAliasTemplate aliasTemplate = requestAliasMap.get(alias);
        if (QueryUtil.isNull(aliasTemplate)) {
            throw new RuntimeException("request: no request alias(" + alias + ") info");
        }

        ReqParam param = new ReqParam();
        Boolean notCount = aliasTemplate.getNotCount();
        if (QueryUtil.isNotNull(notCount)) {
            param.setNotCount(notCount);
        }
        List<List<String>> relationList = aliasTemplate.getRelationList();
        if (QueryUtil.isNotEmpty(relationList)) {
            param.setRelation(relationList);
        }

        ReqAliasTemplateQuery templateQuery = aliasTemplate.getQuery();
        if (QueryUtil.isNotEmpty(query) && QueryUtil.isNotNull(templateQuery)) {
            ReqQuery reqQuery = templateQuery.transfer(query);
            if (QueryUtil.isNotNull(reqQuery)) {
                param.setQuery(reqQuery);
            }
        }
        if (QueryUtil.isNotEmpty(sort)) {
            param.setSort(sort);
        } else {
            Map<String, String> templateSort = aliasTemplate.getSort();
            if (QueryUtil.isNotEmpty(templateSort)) {
                param.setSort(templateSort);
            }
        }
        if (QueryUtil.isNotEmpty(page)) {
            param.setPage(page);
        } else {
            List<String> templatePage = aliasTemplate.getPage();
            if (QueryUtil.isNotEmpty(templatePage)) {
                param.setPage(templatePage);
            }
        }
        return new ReqInfo(aliasTemplate.getTable(), param, aliasTemplate.getType(), aliasTemplate.getResult());
    }
}
