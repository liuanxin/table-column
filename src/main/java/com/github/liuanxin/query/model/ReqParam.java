package com.github.liuanxin.query.model;

import com.github.liuanxin.query.constant.QueryConst;
import com.github.liuanxin.query.util.QueryUtil;

import java.util.*;

/**
 * <pre>
 * WHERE ...
 * ORDER BY create_time DESC, yy.id ASC
 * LIMIT 20
 *
 * {
 *   "query": ...
 *   "sort": { "createTime" : "desc", "yy.id" : "asc" },
 *   "page": [ 1, 20 ],
 *   "notCount": true  -- 当 page 不为空时, true 表示不发起 SELECT COUNT(*) 查询, 不设置则默认是 false
 * }
 * </pre>
 */
public class ReqParam {

    /** 查询信息 */
    private ParamWhere query;
    /** 排序信息 */
    private Map<String, String> sort;
    /** 分页信息 ( 当前页, 每页行数 ), 每页行数在「10, 20, 50, 100, 200, 500, 1000」中, 省略则默认是 10 */
    private List<Integer> page;
    /** 当上面的分页信息有值且当前值是 true 时表示不发起 SELECT COUNT(*) 查询 */
    private Boolean notCount;

    public ReqParam() {}
    public ReqParam(ParamWhere query, Map<String, String> sort, List<Integer> page, Boolean notCount) {
        this.query = query;
        this.sort = sort;
        this.page = page;
        this.notCount = notCount;
    }

    public ParamWhere getQuery() {
        return query;
    }
    public void setQuery(ParamWhere query) {
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

    public Boolean getNotCount() {
        return notCount;
    }
    public void setNotCount(Boolean notCount) {
        this.notCount = notCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReqParam)) return false;
        ReqParam reqParam = (ReqParam) o;
        return Objects.equals(query, reqParam.query) && Objects.equals(sort, reqParam.sort)
                && Objects.equals(page, reqParam.page) && Objects.equals(notCount, reqParam.notCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, sort, page, notCount);
    }

    @Override
    public String toString() {
        return "ReqParam{" +
                "query=" + query +
                ", sort=" + sort +
                ", page=" + page +
                ", notCount=" + notCount +
                '}';
    }


    public Set<String> checkParam(String mainTable, TableColumnInfo tcInfo, int maxListCount) {
        Set<String> paramTableSet = new LinkedHashSet<>();
        if (QueryUtil.isNotNull(query)) {
            paramTableSet.addAll(query.checkCondition(mainTable, tcInfo, maxListCount));
        }
        if (QueryUtil.isEmpty(paramTableSet) && QueryUtil.isEmpty(page)) {
            throw new RuntimeException("param: required query or paging");
        }

        if (QueryUtil.isNotEmpty(sort)) {
            List<String> noTableList = new ArrayList<>();
            List<String> noColumnList = new ArrayList<>();
            for (String column : sort.keySet()) {
                String tableName = QueryUtil.getTableName(column, mainTable);
                Table table = tcInfo.findTableWithAlias(tableName);
                if (QueryUtil.isNull(table)) {
                    noTableList.add(column);
                } else {
                    if (QueryUtil.isNull(tcInfo.findTableColumnWithAlias(table, QueryUtil.getColumnName(column)))) {
                        noColumnList.add(column);
                    } else {
                        paramTableSet.add(table.getName());
                    }
                }
            }
            if (QueryUtil.isNotEmpty(noTableList)) {
                throw new RuntimeException("param sort: table " + noTableList + " has no defined");
            }
            if (QueryUtil.isNotEmpty(noColumnList)) {
                throw new RuntimeException("param sort: column " + noColumnList + " has no defined");
            }
        }

        if (needQueryPage()) {
            Integer index = (page.size() > 0) ? page.get(0) : null;
            if (QueryUtil.isNull(index) || index <= 0) {
                throw new RuntimeException("param page-index error, required > 0");
            }

            if (page.size() > 1) {
                Integer limit = QueryUtil.toInteger(page.get(1));
                if (QueryUtil.isNull(limit) || !QueryConst.LIMIT_SET.contains(limit)) {
                    throw new RuntimeException("param page-limit error, just in " + QueryConst.LIMIT_SET);
                }
            }
        }
        return paramTableSet;
    }

    public String generateWhereSql(String mainTable, TableColumnInfo tcInfo, boolean needAlias, List<Object> params,
                                   Set<String> useTableSet, boolean force, StringBuilder printSql) {
        Set<String> tableSet = new LinkedHashSet<>();
        tableSet.add(tcInfo.findTable(mainTable).getName());
        if (QueryUtil.isNotEmpty(useTableSet)) {
            tableSet.addAll(useTableSet);
        }
        StringBuilder logicDelete = new StringBuilder();
        for (String t : tableSet) {
            logicDelete.append(tcInfo.findTable(t).logicDeleteCondition(force, needAlias));
        }
        boolean emptyLogic = (logicDelete.length() == 0);

        String ld = logicDelete.toString().replaceFirst(" AND ", "");
        if (QueryUtil.isNull(query)) {
            if (emptyLogic) {
                return "";
            } else {
                printSql.append(" WHERE ").append(ld);
                return " WHERE " + ld;
            }
        }

        StringBuilder wherePrint = new StringBuilder();
        String where = query.generateSql(mainTable, tcInfo, needAlias, params, wherePrint);
        if (QueryUtil.isEmpty(where)) {
            if (emptyLogic) {
                return "";
            } else {
                printSql.append(" WHERE ").append(ld);
                return " WHERE " + ld;
            }
        }

        printSql.append(" WHERE ");
        if (emptyLogic) {
            printSql.append(wherePrint);
        } else {
            printSql.append("( ").append(wherePrint).append(" )").append(logicDelete);
        }
        return " WHERE " + (emptyLogic ? where : ("( " + where + " )" + logicDelete));
    }

    public String generateOrderSql(String mainTable, boolean needAlias, TableColumnInfo tcInfo) {
        if (QueryUtil.isNotEmpty(sort)) {
            StringJoiner orderSj = new StringJoiner(", ");
            for (Map.Entry<String, String> entry : sort.entrySet()) {
                String value = entry.getValue().toLowerCase();
                String desc = ("asc".equals(value) || "a".equals(value)) ? "" : " DESC";
                orderSj.add(QueryUtil.getColumnAlias(needAlias, entry.getKey(), mainTable, tcInfo) + desc);
            }
            String orderBy = orderSj.toString();
            if (QueryUtil.isNotEmpty(orderBy)) {
                return " ORDER BY " + orderBy;
            }
        }
        return "";
    }

    public boolean needQueryPage() {
        return QueryUtil.isNotNull(page);
    }
    public boolean needQueryCount() {
        return QueryUtil.isNull(notCount) || !notCount;
    }
    public boolean needQueryCurrentPage(long count) {
        if (count <= 0) {
            return false;
        }
        int index = calcIndex();
        if (index == 1) {
            return true;
        }
        int limit = calcLimit();
        // 比如总条数有 100 条, index 是 11, limit 是 10, 这时候是没必要发起 limit 查询的, 只有 index 在 1 ~ 10 才需要
        return ((long) index * limit) <= count;
    }
    private int calcIndex() {
        return page.get(0);
    }
    private int calcLimit() {
        return (page.size() > 1) ? page.get(1) : QueryConst.DEFAULT_LIMIT;
    }
    public String generatePageSql(List<Object> params, StringBuilder printSql) {
        if (needQueryPage()) {
            int index = calcIndex();
            int limit = calcLimit();

            if (index == 1) {
                params.add(limit);
                printSql.append(" LIMIT ").append(limit);
                return " LIMIT ?";
            } else {
                params.add((index - 1) * limit);
                params.add(limit);
                printSql.append(" LIMIT ").append((index - 1) * limit).append(", ").append(limit);
                return " LIMIT ?, ?";
            }
        }
        return "";
    }
    public String generateArrToObjSql(List<Object> params, StringBuilder printSql) {
        params.add(1);
        printSql.append(" LIMIT 1");
        return " LIMIT ?";
    }

    public boolean hasDeepPage(int maxSize) {
        return needQueryPage() && (((calcIndex() - 1) * calcLimit()) > maxSize);
    }
}
