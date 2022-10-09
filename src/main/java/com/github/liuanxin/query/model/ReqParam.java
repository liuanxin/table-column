package com.github.liuanxin.query.model;

import com.github.liuanxin.query.constant.QueryConst;
import com.github.liuanxin.query.util.QueryUtil;

import java.util.*;

/**
 * <pre>
 * FROM order
 * WHERE ...
 * ORDER BY create_time DESC, id ASC
 * LIMIT 20
 * {
 *   "query": ...
 *   "sort": { "createTime": "desc", "id", "asc" },
 *   "page": [ 1, 20 ],
 *   "noCount": true  -- true 表示不发起 SELECT COUNT(*) 查询(移动端瀑布流时有用), 不设置则默认是 false
 * }
 * </pre>
 */
public class ReqParam {

    /** 查询信息 */
    private ReqParamOperate query;
    /** 排序信息 */
    private Map<String, String> sort;
    /** 分页信息 [ 当前页, 每页行数 ], 每页行数在「10, 20, 50, 100, 200, 500, 1000」中, 省略则默认是 10 */
    private List<Integer> page;
    /** 当上面的分页信息有值且当前值是 true 时表示不发起 SELECT COUNT(*) 查询 */
    private Boolean noCount;

    public ReqParam() {
    }

    public ReqParam(ReqParamOperate query, Map<String, String> sort, List<Integer> page, Boolean noCount) {
        this.query = query;
        this.sort = sort;
        this.page = page;
        this.noCount = noCount;
    }

    public ReqParamOperate getQuery() {
        return query;
    }

    public void setQuery(ReqParamOperate query) {
        this.query = query;
    }

    public Map<String, String> getSort() {
        return sort;
    }

    public void setSort(Map<String, String> sort) {
        this.sort = sort;
    }

    public List<Integer> getPage() {
        return page;
    }

    public void setPage(List<Integer> page) {
        this.page = page;
    }

    public Boolean getNoCount() {
        return noCount;
    }

    public void setNoCount(Boolean noCount) {
        this.noCount = noCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReqParam)) return false;
        ReqParam reqParam = (ReqParam) o;
        return Objects.equals(query, reqParam.query) && Objects.equals(sort, reqParam.sort)
                && Objects.equals(page, reqParam.page) && Objects.equals(noCount, reqParam.noCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, sort, page, noCount);
    }

    @Override
    public String toString() {
        return "ReqParam{" +
                "query=" + query +
                ", sort=" + sort +
                ", page=" + page +
                ", noCount=" + noCount +
                '}';
    }


    public Set<String> checkParam(String mainSchema, SchemaColumnInfo scInfo) {
        Set<String> paramSchemaSet = new LinkedHashSet<>();
        if (query != null) {
            paramSchemaSet.addAll(query.checkCondition(mainSchema, scInfo));
        }

        if (sort != null && !sort.isEmpty()) {
            for (String column : sort.keySet()) {
                String schemaName = QueryUtil.getSchemaName(column, mainSchema);
                Schema schema = scInfo.findSchema(schemaName);
                if (schema == null) {
                    throw new RuntimeException("param sort(" + column + ") has no defined schema");
                }
                if (scInfo.findSchemaColumn(schema, QueryUtil.getColumnName(column)) == null) {
                    throw new RuntimeException("param sort(" + column + ") has no defined column");
                }
                paramSchemaSet.add(schema.getName());
            }
        }

        if (needQueryPage()) {
            Integer indexParam = page.get(0);
            if (indexParam == null || indexParam <= 0) {
                throw new RuntimeException("param page error");
            }
        }
        return paramSchemaSet;
    }

    public String generateWhereSql(String mainSchema, SchemaColumnInfo scInfo, boolean needAlias, List<Object> params) {
        if (query == null) {
            return "";
        } else {
            String where = query.generateSql(mainSchema, scInfo, needAlias, params);
            return where.isEmpty() ? "" : (" WHERE " + where);
        }
    }

    public String generateOrderSql(String mainSchema, boolean needAlias, SchemaColumnInfo scInfo) {
        if (sort != null && !sort.isEmpty()) {
            StringJoiner orderSj = new StringJoiner(", ");
            for (Map.Entry<String, String> entry : sort.entrySet()) {
                String value = entry.getValue().toLowerCase();
                String desc = ("asc".equals(value) || "a".equals(value)) ? "" : " DESC";
                orderSj.add(QueryUtil.getUseColumn(needAlias, entry.getKey(), mainSchema, scInfo) + desc);
            }
            String orderBy = orderSj.toString();
            if (!orderBy.isEmpty()) {
                return " ORDER BY " + orderBy;
            }
        }
        return "";
    }

    public boolean needQueryPage() {
        return page != null && !page.isEmpty();
    }
    public boolean needQueryCount() {
        return noCount == null || !noCount;
    }
    public boolean needQueryCurrentPage(long count) {
        if (count <= 0) {
            return false;
        }
        int index = page.get(0);
        int limit = calcLimit();
        // 比如总条数有 100 条, index 是 11, limit 是 10, 这时候是没必要发起 limit 查询的, 只有 index 在 1 ~ 10 才需要
        return ((long) index * limit) <= count;
    }
    private int calcLimit() {
        Integer limitParam = page.size() > 1 ? page.get(1) : 0;
        return QueryConst.LIMIT_SET.contains(limitParam) ? limitParam : QueryConst.MIN_LIMIT;
    }
    public String generatePageSql(List<Object> params) {
        if (needQueryPage()) {
            int index = page.get(0);
            int limit = calcLimit();

            if (index == 1) {
                params.add(limit);
                return " LIMIT ?";
            } else {
                params.add((index - 1) * limit);
                params.add(limit);
                return " LIMIT ?, ?";
            }
        }
        return "";
    }
    public String generateArrToObjSql(List<Object> params) {
        params.add(1);
        return " LIMIT ?";
    }

    public boolean hasDeepPage(int maxSize) {
        return needQueryPage() && (((page.get(0) - 1) * calcLimit()) > maxSize);
    }
}
