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
 *         "table": "orderAddress",
 *         "columns": [ "id", "address", "phone" ]
 *       },
 *       "items": {
 *         "table": "orderItem",
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
 *     [ "mno", "max", "create_time", [ "yyyy-MM-dd HH:mm", "GMT+8" ] ],
 *     [ "pqr", "avg", "price" ],
 *     [ "stu", "group_concat", "name", "lks", "aaa" ]
 *   ]
 * }
 * 第一个参数表示接口响应回去时的属性, 第二个参数是函数(只支持 COUNT SUM MIN MAX AVG GROUP_CONCAT 这几种)
 * 第三个参数是函数中的列, 每四个和第五个参数表示 HAVING 过滤时的条件
 * </pre>
 */
public class ReqResult {

    /** 表 */
    private String table;
    /** 表里的列 */
    private List<Object> columns;

    public ReqResult() {}
    public ReqResult(String table, List<Object> columns) {
        this.table = table;
        this.columns = columns;
    }

    public String getTable() {
        return table;
    }
    public void setTable(String table) {
        this.table = table;
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
        return Objects.equals(table, reqResult.table) && Objects.equals(columns, reqResult.columns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, columns);
    }

    @Override
    public String toString() {
        return "ReqResult{" +
                "table='" + table + '\'' +
                ", columns=" + columns +
                '}';
    }


    public Set<String> checkResult(String mainTable, TableColumnInfo tcInfo) {
        String currentTable;
        if (QueryUtil.isNotEmpty(table)) {
            currentTable = table;
            Table tableInfo = tcInfo.findTable(currentTable);
            if (QueryUtil.isNull(tableInfo)) {
                throw new RuntimeException("result: has no defined table(" + currentTable + ")");
            }
        } else {
            currentTable = mainTable;
        }
        if (QueryUtil.isEmpty(columns)) {
            throw new RuntimeException("result: table(" + currentTable + ") need columns");
        }

        Set<String> allTableSet = new LinkedHashSet<>();
        Set<String> columnCheckRepeatedSet = new HashSet<>();
        List<Object> innerList = new ArrayList<>();
        boolean hasColumnOrFunction = false;
        for (Object obj : columns) {
            if (obj != null) {
                if (obj instanceof String) {
                    String column = (String) obj;
                    allTableSet.add(checkColumn(column, currentTable, tcInfo, columnCheckRepeatedSet).getName());
                    hasColumnOrFunction = true;
                } else if (obj instanceof List<?>) {
                    List<?> groups = (List<?>) obj;
                    if (QueryUtil.isEmpty(groups)) {
                        throw new RuntimeException("result: table(" + currentTable + ") function(" + groups + ") error");
                    }
                    int size = groups.size();
                    if (size < 3) {
                        throw new RuntimeException("result: table(" + currentTable + ") function(" + groups + ") data error");
                    }
                    ResultGroup group = ResultGroup.deserializer(QueryUtil.toStr(groups.get(1)));
                    if (QueryUtil.isNull(group)) {
                        throw new RuntimeException("result: table(" + currentTable + ") function(" + groups + ") type error");
                    }
                    String column = QueryUtil.toStr(groups.get(2));
                    if (QueryUtil.isEmpty(column)) {
                        throw new RuntimeException("result: table(" + currentTable + ") function(" + groups + ") column error");
                    }

                    if (group == ResultGroup.COUNT_DISTINCT) {
                        for (String col : column.split(",")) {
                            checkFunctionColumn(tcInfo, col, currentTable, groups, allTableSet);
                        }
                    } else {
                        if (group.needCheckColumn(column)) {
                            checkFunctionColumn(tcInfo, column, currentTable, groups, allTableSet);
                        }
                    }

                    String functionColumn = group.generateColumn(column);
                    if (columnCheckRepeatedSet.contains(functionColumn)) {
                        throw new RuntimeException("result: table(" + currentTable + ") function(" + groups + ") has repeated");
                    }
                    columnCheckRepeatedSet.add(functionColumn);

                    if (size > 4) {
                        // 先右移 1 位除以 2, 再左移 1 位乘以 2, 变成偶数
                        int evenSize = size >> 1 << 1;
                        for (int i = 3; i < evenSize; i += 2) {
                            ConditionType conditionType = ConditionType.deserializer(groups.get(i));
                            if (conditionType == null) {
                                throw new RuntimeException("result: table(" + currentTable + ") function("
                                        + groups + ") having condition error");
                            }

                            Object value = groups.get(i + 1);
                            if (group.checkNotHavingValue(value)) {
                                throw new RuntimeException("result: table(" + currentTable + ") function("
                                        + groups + ") having condition value(" + value + ") type error");
                            }
                        }
                    }
                    hasColumnOrFunction = true;
                } else {
                    Map<String, List<String>> dateColumn = QueryJsonUtil.convertDateResult(obj);
                    if (dateColumn != null) {
                        for (String column : dateColumn.keySet()) {
                            allTableSet.add(checkColumn(column, currentTable, tcInfo, columnCheckRepeatedSet).getName());
                        }
                        hasColumnOrFunction = true;
                    } else {
                        innerList.add(obj);
                    }
                }
            }
        }
        if (!hasColumnOrFunction) {
            throw new RuntimeException("result: table(" + currentTable + ") no columns");
        }

        for (Object obj : innerList) {
            Map<String, ReqResult> inner = QueryJsonUtil.convertInnerResult(obj);
            if (inner == null) {
                throw new RuntimeException("result: table(" + currentTable + ") relation(" + obj + ") error");
            }
            for (Map.Entry<String, ReqResult> entry : inner.entrySet()) {
                String innerColumn = entry.getKey();
                ReqResult innerResult = entry.getValue();
                if (innerResult == null) {
                    throw new RuntimeException("result: table(" + mainTable + ") inner(" + innerColumn + ") error");
                }
                if (columnCheckRepeatedSet.contains(innerColumn)) {
                    throw new RuntimeException("result: table(" + mainTable + ") inner(" + innerColumn + ") has repeated");
                }
                columnCheckRepeatedSet.add(innerColumn);

                String innerTable = innerResult.getTable();
                if (QueryUtil.isEmpty(innerTable)) {
                    throw new RuntimeException("result: table(" + mainTable + ") inner(" + innerColumn + ") need table");
                }
                Table table = tcInfo.findTable(innerTable);
                if (QueryUtil.isNull(table)) {
                    throw new RuntimeException("result: table(" + mainTable + ") inner(" + innerColumn + ") has no defined table");
                }

                TableColumnRelation relation = tcInfo.findRelationByMasterChild(mainTable, innerTable);
                if (QueryUtil.isNull(relation)) {
                    relation = tcInfo.findRelationByMasterChild(innerTable, mainTable);
                }
                if (QueryUtil.isNull(relation)) {
                    throw new RuntimeException("result: " + mainTable + " - " + innerColumn + "(" + innerTable + ") has no relation");
                }
                Set<String> innerTableSet = innerResult.checkResult(mainTable, tcInfo);
                if (innerTableSet.size() > 1) {
                    throw new RuntimeException("result: " + mainTable + " - " + innerColumn + "(" + innerTable + ") just has one Table to Query");
                }
            }
        }
        return allTableSet;
    }

    private static void checkFunctionColumn(TableColumnInfo tcInfo, String column, String currentTable,
                                            List<?> groups, Set<String> allTableSet) {
        Table sa = tcInfo.findTable(QueryUtil.getTableName(column, currentTable));
        if (sa == null) {
            throw new RuntimeException("result: table(" + currentTable + ") function(" + groups + ") has no defined table");
        }
        if (tcInfo.findTableColumn(sa, QueryUtil.getColumnName(column)) == null) {
            throw new RuntimeException("result: table(" + currentTable + ") function(" + groups + ") has no defined column");
        }
        allTableSet.add(sa.getName());
    }

    private Table checkColumn(String column, String currentTable, TableColumnInfo tcInfo, Set<String> columnSet) {
        if (QueryUtil.isEmpty(column)) {
            throw new RuntimeException("result: table(" + currentTable + ") column can't be blank");
        }

        Table sa = tcInfo.findTable(QueryUtil.getTableName(column, currentTable));
        if (sa == null) {
            throw new RuntimeException("result: table(" + currentTable + ") column(" + column + ") has no defined table");
        }
        if (tcInfo.findTableColumn(sa, QueryUtil.getColumnName(column)) == null) {
            throw new RuntimeException("result: table(" + currentTable + ") column(" + column + ") has no defined column");
        }

        if (columnSet.contains(column)) {
            throw new RuntimeException("result: table(" + currentTable + ") column(" + column + ") has repeated");
        }
        columnSet.add(column);
        return sa;
    }

    public String generateAllSelectSql(String mainTable, TableColumnInfo tcInfo, boolean needAlias) {
        Set<String> columnNameSet = new LinkedHashSet<>();
        columnNameSet.addAll(selectColumn(mainTable, tcInfo, needAlias));
        columnNameSet.addAll(innerColumn(mainTable, tcInfo, needAlias));
        return String.join(", ", columnNameSet);
    }

    private Set<String> selectColumn(String mainTable, TableColumnInfo tcInfo, boolean needAlias) {
        Set<String> columnNameSet = new LinkedHashSet<>();
        String currentTableName = QueryUtil.isEmpty(table) ? mainTable : table;
        for (Object obj : columns) {
            if (obj instanceof String) {
                String col = (String) obj;
                columnNameSet.add(QueryUtil.getQueryColumnAndAlias(needAlias, col, currentTableName, tcInfo));
            } else {
                Map<String, List<String>> dateColumn = QueryJsonUtil.convertDateResult(obj);
                if (dateColumn != null) {
                    for (String column : dateColumn.keySet()) {
                        columnNameSet.add(QueryUtil.getQueryColumnAndAlias(needAlias, column, currentTableName, tcInfo));
                    }
                }
            }
        }
        return columnNameSet;
    }

    private Set<String> innerColumn(String mainTable, TableColumnInfo tcInfo, boolean needAlias) {
        Set<String> columnNameSet = new LinkedHashSet<>();
        String currentTable = QueryUtil.isEmpty(table) ? mainTable : table;
        for (ReqResult innerResult : innerResult().values()) {
            // child-master or master-child all need to query masterId
            String innerTable = innerResult.getTable();
            TableColumnRelation relation = tcInfo.findRelationByMasterChild(currentTable, innerTable);
            if (relation == null) {
                relation = tcInfo.findRelationByMasterChild(innerTable, currentTable);
            }
            if (relation != null) {
                String column = relation.getOneColumn();
                columnNameSet.add(QueryUtil.getQueryColumnAndAlias(needAlias, column, currentTable, tcInfo));
            }
        }
        return columnNameSet;
    }

    public Set<String> needRemoveColumn(String mainTable, TableColumnInfo tcInfo, boolean needAlias) {
        Set<String> selectColumnSet = selectColumn(mainTable, tcInfo, needAlias);
        Set<String> removeColumnSet = new HashSet<>();
        for (String ic : innerColumn(mainTable, tcInfo, needAlias)) {
            if (!selectColumnSet.contains(ic)) {
                removeColumnSet.add(calcRemoveColumn(ic));
            }
        }
        return removeColumnSet;
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

    public String generateFunctionSql(String mainTable, boolean needAlias, TableColumnInfo tcInfo) {
        StringJoiner sj = new StringJoiner(", ");
        for (Object obj : columns) {
            if (obj instanceof List<?>) {
                sj.add(generateFunctionColumn((List<?>) obj, mainTable, needAlias, tcInfo));
            }
        }
        return sj.toString();
    }
    private String generateFunctionColumn(List<?> groups, String mainTable, boolean needAlias, TableColumnInfo tcInfo) {
        String column = QueryUtil.toStr(groups.get(2));
        ResultGroup group = ResultGroup.deserializer(QueryUtil.toStr(groups.get(1)));
        if (group == ResultGroup.COUNT_DISTINCT) {
            StringJoiner funSj = new StringJoiner(", ");
            for (String col : column.split(",")) {
                funSj.add(QueryUtil.getQueryColumn(needAlias, col, mainTable, tcInfo));
            }
            return group.generateColumn(funSj.toString());
        } else {
            return group.generateColumn(QueryUtil.getQueryColumn(needAlias, column, mainTable, tcInfo));
        }
    }

    public boolean needGroup() {
        boolean hasColumn = false;
        boolean hasGroup = false;
        for (Object obj : columns) {
            if (obj instanceof String) {
                String column = (String) obj;
                if (QueryUtil.isNotEmpty(column)) {
                    hasColumn = true;
                }
            } else if (obj instanceof List<?>) {
                if (QueryUtil.isNotEmpty((List<?>) obj)) {
                    hasGroup = true;
                }
            }
        }
        return hasColumn && hasGroup;
    }
    public String generateGroupSql(String mainTable, boolean needAlias, TableColumnInfo tcInfo) {
        StringJoiner sj = new StringJoiner(", ");
        boolean hasGroup = false;
        for (Object obj : columns) {
            if (obj instanceof String) {
                String column = (String) obj;
                if (QueryUtil.isNotEmpty(column)) {
                    sj.add(QueryUtil.getColumnAlias(needAlias, column, mainTable, tcInfo));
                }
            } else if (obj instanceof List<?>) {
                if (QueryUtil.isNotEmpty((List<?>) obj)) {
                    hasGroup = true;
                }
            }
        }
        return (hasGroup && sj.length() > 0) ? (" GROUP BY " + sj) : "";
    }

    public String generateHavingSql(String mainTable, boolean needAlias, TableColumnInfo tcInfo,
                                    List<Object> params, StringBuilder printSql) {
        // 只支持 AND 条件过滤, 复杂的嵌套暂没有想到好的抽象方式
        StringJoiner groupSj = new StringJoiner(" AND ");
        for (Object obj : columns) {
            if (obj instanceof List<?>) {
                List<?> groups = (List<?>) obj;
                int size = groups.size();
                if (size > 4) {
                    String column = QueryUtil.toStr(groups.get(2));
                    ResultGroup group = ResultGroup.deserializer(QueryUtil.toStr(groups.get(1)));
                    String groupAlias = group.generateAlias(QueryUtil.getQueryColumn(needAlias, column, mainTable, tcInfo));

                    String tableName = QueryUtil.getTableName(column, mainTable);
                    String columnName = QueryUtil.getColumnName(column);
                    Class<?> fieldType = tcInfo.findTableColumn(tableName, columnName).getFieldType();
                    // 先右移 1 位除以 2, 再左移 1 位乘以 2, 变成偶数
                    int evenSize = size >> 1 << 1;
                    for (int i = 3; i < evenSize; i += 2) {
                        ConditionType conditionType = ConditionType.deserializer(groups.get(i));
                        Object value = groups.get(i + 1);

                        String sql = conditionType.generateSql(groupAlias, fieldType, value, params, printSql);
                        if (QueryUtil.isNotEmpty(sql)) {
                            groupSj.add(sql);
                        }
                    }
                }
            }
        }
        return (groupSj.length() == 0) ? "" : (" HAVING " + groupSj);
    }

    public String generateInnerSelect(String relationColumn, TableColumnInfo tcInfo, Set<String> removeColumn) {
        String innerTableName = table;

        Set<String> columnSet = new LinkedHashSet<>();
        for (Object obj : columns) {
            if (obj instanceof String) {
                columnSet.add(QueryUtil.getQueryColumnAndAlias(false, (String) obj, innerTableName, tcInfo));
            } else if (obj instanceof List<?>) {
                columnSet.add(generateFunctionColumn((List<?>) obj, innerTableName, false, tcInfo));
            } else {
                Map<String, List<String>> dateColumn = QueryJsonUtil.convertDateResult(obj);
                if (dateColumn != null) {
                    for (String column : dateColumn.keySet()) {
                        String tableName = QueryUtil.getTableName(column, innerTableName);
                        if (tableName.equals(innerTableName)) {
                            columnSet.add(QueryUtil.getQueryColumnAndAlias(false, column, innerTableName, tcInfo));
                        }
                    }
                } else {
                    Map<String, ReqResult> inner = QueryJsonUtil.convertInnerResult(obj);
                    if (inner != null) {
                        for (ReqResult innerInnerResult : inner.values()) {
                            String innerInnerTable = innerInnerResult.getTable();
                            TableColumnRelation relation = tcInfo.findRelationByMasterChild(innerTableName, innerInnerTable);
                            if (relation == null) {
                                relation = tcInfo.findRelationByMasterChild(innerInnerTable, innerTableName);
                            }
                            if (relation != null) {
                                String column = relation.getOneColumn();
                                columnSet.add(QueryUtil.getQueryColumnAndAlias(false, column, innerTableName, tcInfo));
                            }
                        }
                    }
                }
            }
        }

        String relationId = QueryUtil.getQueryColumnAndAlias(false, relationColumn, innerTableName, tcInfo);
        if (!columnSet.contains(relationId)) {
            removeColumn.add(calcRemoveColumn(relationId));
        }

        List<String> columnList = new ArrayList<>();
        columnList.add(relationId);
        columnList.addAll(columnSet);
        return String.join(", ", columnList);
    }

    private String calcRemoveColumn(String columnAndAlias) {
        String as = " AS ";
        if (columnAndAlias.contains(as)) {
            return columnAndAlias.substring(columnAndAlias.indexOf(as) + as.length());
        } else {
            String point = ".";
            if (columnAndAlias.contains(point)) {
                return columnAndAlias.substring(columnAndAlias.indexOf(point) + point.length());
            } else {
                return columnAndAlias;
            }
        }
    }


    public void handleData(String mainTable, boolean needAlias, Map<String, Object> data, TableColumnInfo tcInfo) {
        String currentTable = QueryUtil.isEmpty(table) ? mainTable : table;
        for (Object obj : columns) {
            if (obj != null) {
                if (obj instanceof String) {
                    String column = (String) obj;
                    String tableName = QueryUtil.getTableName(column, currentTable);
                    String columnName = QueryUtil.getColumnName(column);
                    Class<?> fieldType = tcInfo.findTableColumn(tableName, columnName).getFieldType();
                    if (Date.class.isAssignableFrom(fieldType)) {
                        Date date = QueryUtil.toDate(data.get(columnName));
                        if (QueryUtil.isNotNull(date)) {
                            data.put(columnName, QueryUtil.formatDate(date));
                        }
                    }
                } else if (obj instanceof List<?>) {
                    List<?> groups = (List<?>) obj;
                    ResultGroup group = ResultGroup.deserializer(QueryUtil.toStr(groups.get(1)));
                    String column = QueryUtil.toStr(groups.get(2));
                    String useColumn = QueryUtil.getQueryColumn(needAlias, column, mainTable, tcInfo);
                    Object groupInfo = data.remove(group.generateAlias(useColumn));
                    if (QueryUtil.isNotNull(groupInfo)) {
                        String returnColumn = QueryUtil.toStr(groups.get(0));
                        Date date = QueryUtil.toDate(groupInfo);
                        if (QueryUtil.isNotNull(date)) {
                            String dateInfo = null;
                            int size = groups.size();
                            if (size > 3) {
                                List<String> values = QueryJsonUtil.convertList(groups.get(size - 1), String.class);
                                if (QueryUtil.isNotEmpty(values)) {
                                    String pattern = values.get(0);
                                    String timezone = (values.size() > 1) ? values.get(1) : null;
                                    dateInfo = QueryUtil.formatDate(date, pattern, timezone);
                                }
                            }
                            if (QueryUtil.isEmpty(dateInfo)) {
                                dateInfo = QueryUtil.formatDate(date);
                            }
                            data.put(returnColumn, dateInfo);
                        } else {
                            data.put(returnColumn, groupInfo);
                        }
                    }
                } else {
                    Map<String, List<String>> dateColumn = QueryJsonUtil.convertDateResult(obj);
                    if (dateColumn != null) {
                        for (Map.Entry<String, List<String>> entry : dateColumn.entrySet()) {
                            List<String> values = entry.getValue();
                            if (QueryUtil.isNotEmpty(values)) {
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
