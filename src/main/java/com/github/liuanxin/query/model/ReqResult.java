package com.github.liuanxin.query.model;

import com.github.liuanxin.query.enums.ConditionType;
import com.github.liuanxin.query.enums.ResultGroup;
import com.github.liuanxin.query.util.QueryJsonUtil;
import com.github.liuanxin.query.util.QueryUtil;

import java.util.*;

/**
 * <pre>
 * 1. FROM & JOINs: determine & filter rows
 * 2. WHERE: more filters on the rows
 * 3. GROUP BY: combines those rows into groups
 * 4. HAVING: filters groups
 * 5. ORDER BY: arranges the remaining rows/groups
 * 6. LIMIT: filters on the remaining rows/groups
 *
 * SELECT id, order_no FROM t_order ...
 * SELECT id, address, phone FROM t_order_address ...
 * SELECT id, name, price FROM t_order_item ...
 * {
 *   "columns": [
 *     "id",
 *     "orderNo",
 *     { "create_time" : [ "yyyy-MM-dd HH:mm", "GMT+8" ] },  -- format date [ "pattern", "timeZone" ]
 *     "update_time",  -- format pattern default: yyyy-MM-dd HH:mm:ss
 *     {
 *       "address": {
 *         "schema": "orderAddress",
 *         "columns": [ "id", "address", "phone" ]
 *       },
 *       "items": {
 *         "schema": "orderItem",
 *         "columns": [ "id", "name", "price" ]
 *       }
 *     }
 *   ]
 * }
 *
 *
 * COUNT(*) 跟 COUNT(1) 是等价的, 使用标准 COUNT(*) 就好了
 * 见: https://dev.mysql.com/doc/refman/8.0/en/aggregate-functions.html#function_count
 *
 * SELECT
 *   name, COUNT(*), COUNT(DISTINCT name, name2), SUM(price),
 *   MIN(id), MAX(id), AVG(price), GROUP_CONCAT(name)
 * ...
 * GROUP BY name
 * HAVING  SUM(price) > 100.5  AND  SUM(price) < 120.5  AND  GROUP_CONCAT(name) LIKE 'aaa%'
 * {
 *   "columns": [
 *     "name",
 *     [ "abc", "count", "*" ],
 *     [ "def", "count_distinct", "name, name2" ],
 *     [ "ghi", "sum", "price", "gt", 100.5, "lt", 120.5 ],
 *     [ "jkl", "min", "id" ],
 *     [ "mno", "max", "id" ],
 *     [ "pqr", "avg", "price" ],
 *     [ "stu", "group_concat", "name", "lks", "aaa" ]
 *   ]
 * }
 * 第一个参数表示接口响应回去时的属性, 第二个参数是函数(只支持上面 6 种), 第三个参数是函数中的列,
 * 每四个和第五个参数表示 HAVING 过滤时的条件, 只支持 AND 条件过滤, 复杂的嵌套暂没有想到好的抽象方式
 * </pre>
 */
public class ReqResult {

    /** 表 */
    private String schema;
    /** 表里的列 */
    private List<Object> columns;

    public ReqResult() {
    }

    public ReqResult(String schema, List<Object> columns) {
        this.schema = schema;
        this.columns = columns;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public List<Object> getColumns() {
        return columns;
    }

    public void setColumns(List<Object> columns) {
        this.columns = columns;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReqResult)) return false;
        ReqResult reqResult = (ReqResult) o;
        return Objects.equals(schema, reqResult.schema) && Objects.equals(columns, reqResult.columns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schema, columns);
    }

    @Override
    public String toString() {
        return "ReqResult{" +
                "schema='" + schema + '\'' +
                ", columns=" + columns +
                '}';
    }


    public Set<String> checkResult(String mainSchema, SchemaColumnInfo scInfo, Set<String> allSchemaSet) {
        String currentSchema = (schema == null || schema.trim().isEmpty()) ? mainSchema : schema.trim();
        Schema schemaInfo = scInfo.findSchema(currentSchema);
        if (schemaInfo == null) {
            throw new RuntimeException("result has no defined schema(" + currentSchema + ")");
        }
        if (columns == null || columns.isEmpty()) {
            throw new RuntimeException("result schema(" + currentSchema + ") need columns");
        }

        Set<String> resultFunctionSchemaSet = new LinkedHashSet<>();
        Set<String> columnCheckRepeatedSet = new HashSet<>();
        List<Object> innerList = new ArrayList<>();
        boolean hasColumnOrFunction = false;
        for (Object obj : columns) {
            if (obj != null) {
                if (obj instanceof String) {
                    String column = (String) obj;
                    allSchemaSet.add(checkColumn(column, currentSchema, scInfo, columnCheckRepeatedSet).getName());
                    hasColumnOrFunction = true;
                } else if (obj instanceof List<?>) {
                    List<?> groups = (List<?>) obj;
                    if (groups.isEmpty()) {
                        throw new RuntimeException("result schema(" + currentSchema + ") function(" + groups + ") error");
                    }
                    int size = groups.size();
                    if (size < 3) {
                        throw new RuntimeException("result schema(" + currentSchema + ") function(" + groups + ") data error");
                    }
                    ResultGroup group = ResultGroup.deserializer(QueryUtil.toStr(groups.get(1)));
                    if (group == null) {
                        throw new RuntimeException("result schema(" + currentSchema + ") function(" + groups + ") type error");
                    }
                    String column = QueryUtil.toStr(groups.get(2));
                    if (column.isEmpty()) {
                        throw new RuntimeException("result schema(" + currentSchema + ") function(" + groups + ") column error");
                    }

                    if (group == ResultGroup.COUNT_DISTINCT) {
                        for (String col : column.split(",")) {
                            Schema sa = scInfo.findSchema(QueryUtil.getSchemaName(col.trim(), currentSchema));
                            if (sa == null) {
                                throw new RuntimeException("result schema(" + currentSchema + ") function(" + groups + ") has no defined schema");
                            }
                            if (scInfo.findSchemaColumn(sa, QueryUtil.getColumnName(col.trim())) == null) {
                                throw new RuntimeException("result schema(" + currentSchema + ") function(" + groups + ") has no defined column");
                            }
                            resultFunctionSchemaSet.add(sa.getName());
                            allSchemaSet.add(sa.getName());
                        }
                    } else {
                        if (!(group == ResultGroup.COUNT && new HashSet<>(Arrays.asList("*", "1")).contains(column))) {
                            Schema sa = scInfo.findSchema(QueryUtil.getSchemaName(column, currentSchema));
                            if (sa == null) {
                                throw new RuntimeException("result schema(" + currentSchema + ") function(" + groups + ") has no defined schema");
                            }
                            if (scInfo.findSchemaColumn(sa, QueryUtil.getColumnName(column)) == null) {
                                throw new RuntimeException("result schema(" + currentSchema + ") function(" + groups + ") has no defined column");
                            }
                            resultFunctionSchemaSet.add(sa.getName());
                            allSchemaSet.add(sa.getName());
                        }
                    }

                    String functionColumn = group.generateColumn(column);
                    if (columnCheckRepeatedSet.contains(functionColumn)) {
                        throw new RuntimeException("result schema(" + currentSchema + ") function(" + groups + ") has repeated");
                    }
                    columnCheckRepeatedSet.add(functionColumn);

                    if (size > 4) {
                        // 先右移 1 位除以 2, 再左移 1 位乘以 2, 变成偶数
                        int evenSize = size >> 1 << 1;
                        for (int i = 3; i < evenSize; i += 2) {
                            ConditionType conditionType = ConditionType.deserializer(groups.get(i));
                            if (conditionType == null) {
                                throw new RuntimeException("result schema(" + currentSchema + ") function("
                                        + groups + ") having condition error");
                            }

                            Object value = groups.get(i + 1);
                            if (group.checkNotHavingValue(value)) {
                                throw new RuntimeException("result schema(" + currentSchema + ") function("
                                        + groups + ") having condition value(" + value + ") type error");
                            }
                        }
                    }
                    hasColumnOrFunction = true;
                } else {
                    Map<String, List<String>> dateColumn = QueryJsonUtil.convertDateResult(obj);
                    if (dateColumn != null) {
                        for (String column : dateColumn.keySet()) {
                            allSchemaSet.add(checkColumn(column, currentSchema, scInfo, columnCheckRepeatedSet).getName());
                        }
                        hasColumnOrFunction = true;
                    } else {
                        innerList.add(obj);
                    }
                }
            }
        }
        if (!hasColumnOrFunction) {
            throw new RuntimeException("result schema(" + currentSchema + ") no columns");
        }

        for (Object obj : innerList) {
            Map<String, ReqResult> inner = QueryJsonUtil.convertInnerResult(obj);
            if (inner == null) {
                throw new RuntimeException("result schema(" + currentSchema + ") relation(" + obj + ") error");
            }
            for (Map.Entry<String, ReqResult> entry : inner.entrySet()) {
                String column = entry.getKey();
                ReqResult innerResult = entry.getValue();
                if (innerResult == null) {
                    throw new RuntimeException("result schema(" + currentSchema + ") relation column(" + column + ") error");
                }
                if (columnCheckRepeatedSet.contains(column)) {
                    throw new RuntimeException("result schema(" + currentSchema + ") relation column(" + column + ") has repeated");
                }
                columnCheckRepeatedSet.add(column);
                String innerSchema = innerResult.getSchema();
                if (innerSchema == null || innerSchema.isEmpty()) {
                    throw new RuntimeException("result schema(" + currentSchema + ") inner(" + column + ") need schema");
                }
                if (scInfo.findRelationByMasterChild(currentSchema, innerSchema) == null) {
                    throw new RuntimeException("result " + currentSchema + " - " + column + " -" + innerSchema + " has no relation");
                }
                innerResult.checkResult(innerSchema, scInfo, allSchemaSet);
            }
        }
        return resultFunctionSchemaSet;
    }
    private Schema checkColumn(String column, String currentSchema, SchemaColumnInfo scInfo, Set<String> columnSet) {
        if (column.trim().isEmpty()) {
            throw new RuntimeException("result schema(" + currentSchema + ") column can't be blank");
        }

        String col = column.trim();
        Schema sa = scInfo.findSchema(QueryUtil.getSchemaName(col, currentSchema));
        if (sa == null) {
            throw new RuntimeException("result schema(" + currentSchema + ") column(" + col + ") has no defined schema");
        }
        if (scInfo.findSchemaColumn(sa, QueryUtil.getColumnName(col)) == null) {
            throw new RuntimeException("result schema(" + currentSchema + ") column(" + col + ") has no defined column");
        }

        if (columnSet.contains(col)) {
            throw new RuntimeException("result schema(" + currentSchema + ") column(" + col + ") has repeated");
        }
        columnSet.add(col);
        return sa;
    }

    public String generateAllSelectSql(String mainSchema, SchemaColumnInfo scInfo, Set<String> schemaSet) {
        Set<String> columnNameSet = new LinkedHashSet<>();
        columnNameSet.addAll(selectColumn(mainSchema, scInfo, schemaSet));
        columnNameSet.addAll(innerColumn(mainSchema, scInfo, !schemaSet.isEmpty()));
        return String.join(", ", columnNameSet);
    }

    public Set<String> selectColumn(String mainSchema, SchemaColumnInfo scInfo, Set<String> schemaSet) {
        Set<String> columnNameSet = new LinkedHashSet<>();
        boolean needAlias = !schemaSet.isEmpty();
        String currentSchemaName = (schema == null || schema.isEmpty()) ? mainSchema : schema.trim();
        columnNameSet.add(scInfo.findSchema(currentSchemaName).idSelect(needAlias));
        for (Object obj : columns) {
            if (obj instanceof String) {
                String col = ((String) obj).trim();
                String schemaName = QueryUtil.getSchemaName(col, currentSchemaName);
                if (schemaName.equals(currentSchemaName) || schemaSet.contains(schemaName)) {
                    columnNameSet.add(QueryUtil.getUseQueryColumn(needAlias, col, currentSchemaName, scInfo));
                }
            } else {
                Map<String, List<String>> dateColumn = QueryJsonUtil.convertDateResult(obj);
                if (dateColumn != null) {
                    for (String column : dateColumn.keySet()) {
                        String col = column.trim();
                        String schemaName = QueryUtil.getSchemaName(col, currentSchemaName);
                        if (schemaName.equals(currentSchemaName) || schemaSet.contains(schemaName)) {
                            columnNameSet.add(QueryUtil.getUseQueryColumn(needAlias, col, currentSchemaName, scInfo));
                        }
                    }
                }
            }
        }
        return columnNameSet;
    }

    public Set<String> innerColumn(String mainSchema, SchemaColumnInfo scInfo, boolean needAlias) {
        Set<String> columnNameSet = new LinkedHashSet<>();
        String currentSchemaName = (schema == null || schema.isEmpty()) ? mainSchema : schema.trim();
        for (Object obj : columns) {
            if (!(obj instanceof String)  && !(obj instanceof List<?>)) {
                Map<String, ReqResult> inner = QueryJsonUtil.convertInnerResult(obj);
                if (inner != null) {
                    for (ReqResult innerResult : inner.values()) {
                        String column = scInfo.findRelationByMasterChild(currentSchemaName, innerResult.getSchema()).getOneColumn();
                        columnNameSet.add(QueryUtil.getUseQueryColumn(needAlias, column, currentSchemaName, scInfo));
                    }
                }
            }
        }
        return columnNameSet;
    }

    public Map<String, ReqResult> innerResult() {
        Map<String, ReqResult> returnMap = new LinkedHashMap<>();
        for (Object obj : columns) {
            if (!(obj instanceof String)  && !(obj instanceof List<?>)) {
                Map<String, ReqResult> inner = QueryJsonUtil.convertInnerResult(obj);
                if (inner != null) {
                    returnMap.putAll(inner);
                }
            }
        }
        return returnMap;
    }

    public String generateFunctionSql(String mainSchema, boolean needAlias, SchemaColumnInfo scInfo) {
        StringJoiner sj = new StringJoiner(", ");
        for (Object obj : columns) {
            if (obj instanceof List<?>) {
                List<?> groups = (List<?>) obj;
                String column = QueryUtil.toStr(groups.get(2));
                ResultGroup group = ResultGroup.deserializer(QueryUtil.toStr(groups.get(1)));
                if (group == ResultGroup.COUNT_DISTINCT) {
                    StringJoiner funSj = new StringJoiner(", ");
                    for (String col : column.split(",")) {
                        funSj.add(QueryUtil.getUseColumn(needAlias, col.trim(), mainSchema, scInfo));
                    }
                    sj.add(group.generateColumn(funSj.toString()));
                } else {
                    sj.add(group.generateColumn(QueryUtil.getUseColumn(needAlias, column, mainSchema, scInfo)));
                }
            }
        }
        return sj.toString();
    }

    public boolean needGroup() {
        boolean hasColumn = false;
        boolean hasGroup = false;
        for (Object obj : columns) {
            if (obj instanceof String) {
                String column = (String) obj;
                if (!column.isEmpty()) {
                    hasColumn = true;
                }
            } else if (obj instanceof List<?>) {
                List<?> groups = (List<?>) obj;
                if (!groups.isEmpty()) {
                    hasGroup = true;
                }
            }
        }
        return hasColumn && hasGroup;
    }
    public String generateGroupSql(String mainSchema, boolean needAlias, SchemaColumnInfo scInfo) {
        StringJoiner sj = new StringJoiner(", ");
        boolean hasGroup = false;
        for (Object obj : columns) {
            if (obj instanceof String) {
                String column = (String) obj;
                if (!column.isEmpty()) {
                    sj.add(QueryUtil.getUseColumn(needAlias, column, mainSchema, scInfo));
                }
            } else if (obj instanceof List<?>) {
                List<?> groups = (List<?>) obj;
                if (!groups.isEmpty()) {
                    hasGroup = true;
                }
            }
        }
        return (hasGroup && sj.length() > 0) ? (" GROUP BY " + sj) : "";
    }

    public String generateHavingSql(String mainSchema, boolean needAlias, SchemaColumnInfo scInfo, List<Object> params) {
        StringJoiner groupSj = new StringJoiner(" AND ");
        for (Object obj : columns) {
            if (obj instanceof List<?>) {
                List<?> groups = (List<?>) obj;
                int size = groups.size();
                if (size > 4) {
                    String column = QueryUtil.toStr(groups.get(2));
                    ResultGroup group = ResultGroup.deserializer(QueryUtil.toStr(groups.get(1)));
                    String useColumn = QueryUtil.getUseColumn(needAlias, column, mainSchema, scInfo);
                    String havingColumn = group.generateColumn(useColumn);

                    String schemaName = QueryUtil.getSchemaName(column, mainSchema);
                    String columnName = QueryUtil.getColumnName(column);
                    Class<?> columnType = scInfo.findSchemaColumn(schemaName, columnName).getColumnType();
                    // 先右移 1 位除以 2, 再左移 1 位乘以 2, 变成偶数
                    int evenSize = size >> 1 << 1;
                    for (int i = 3; i < evenSize; i += 2) {
                        ConditionType conditionType = ConditionType.deserializer(groups.get(i));
                        Object value = groups.get(i + 1);

                        String sql = conditionType.generateSql(havingColumn, columnType, value, params);
                        if (!sql.isEmpty()) {
                            groupSj.add(sql);
                        }
                    }
                }
            }
        }
        String groupBy = groupSj.toString();
        return groupBy.isEmpty() ? "" : (" HAVING " + groupBy);
    }


    public void handleDateType(Map<String, Object> data, String mainSchema, SchemaColumnInfo scInfo) {
        String currentSchemaName = (schema == null || schema.isEmpty()) ? mainSchema : schema.trim();
        for (Object obj : columns) {
            if (obj != null) {
                if (obj instanceof String) {
                    String column = (String) obj;
                    String schemaName = QueryUtil.getSchemaName(column, currentSchemaName);
                    String columnName = QueryUtil.getColumnName(column);
                    Class<?> columnType = scInfo.findSchemaColumn(schemaName, columnName).getColumnType();
                    if (Date.class.isAssignableFrom(columnType)) {
                        Date date = QueryUtil.toDate(data.get(columnName));
                        if (date != null) {
                            data.put(columnName, QueryUtil.formatDate(date));
                        }
                    }
                } else if (!(obj instanceof List<?>)) {
                    Map<String, List<String>> dateColumn = QueryJsonUtil.convertDateResult(obj);
                    if (dateColumn != null) {
                        for (Map.Entry<String, List<String>> entry : dateColumn.entrySet()) {
                            List<String> values = entry.getValue();
                            if (values != null && !values.isEmpty()) {
                                Date date = QueryUtil.toDate(data.get(entry.getKey()));
                                if (date != null) {
                                    String pattern = values.get(0);
                                    String timezone = (values.size() > 1) ? values.get(1) : null;
                                    data.put(entry.getKey(), QueryUtil.formatDate(date, pattern, timezone));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
