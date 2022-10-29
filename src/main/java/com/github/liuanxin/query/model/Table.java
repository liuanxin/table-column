package com.github.liuanxin.query.model;

import com.github.liuanxin.query.constant.QueryConst;
import com.github.liuanxin.query.util.QuerySqlUtil;
import com.github.liuanxin.query.util.QueryUtil;

import java.lang.reflect.Field;
import java.util.*;

public class Table {

    /** table name */
    private String name;

    /** table desc */
    private String desc;

    /** table alias */
    private String alias;

    /** logic delete column name */
    private String logicColumn;

    /** logic delete column default value. for example: 0 */
    private String logicValue;

    /** logic delete column delete value. for example: 1 or id or UNIX_TIMESTAMP() */
    private String logicDeleteValue;

    /** column mapping info */
    private Map<String, TableColumn> columnMap;

    /** primary key */
    private List<String> idKey;

    public Table() {}
    public Table(String name, String desc, String alias,
                 String logicColumn, String logicValue, String logicDeleteValue,
                 Map<String, TableColumn> columnMap) {
        this.name = name;
        this.desc = desc;
        this.alias = alias;
        this.logicColumn = logicColumn;
        this.logicValue = logicValue;
        this.logicDeleteValue = logicDeleteValue;
        this.columnMap = columnMap;

        List<String> idKey = new ArrayList<>();
        if (!columnMap.isEmpty()) {
            for (TableColumn tableColumn : columnMap.values()) {
                if (tableColumn.isPrimary()) {
                    idKey.add(tableColumn.getName());
                }
            }
        }
        this.idKey = idKey;
    }


    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }
    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getAlias() {
        return alias;
    }
    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getLogicColumn() {
        return logicColumn;
    }
    public void setLogicColumn(String logicColumn) {
        this.logicColumn = logicColumn;
    }

    public String getLogicValue() {
        return logicValue;
    }
    public void setLogicValue(String logicValue) {
        this.logicValue = logicValue;
    }

    public String getLogicDeleteValue() {
        return logicDeleteValue;
    }
    public void setLogicDeleteValue(String logicDeleteValue) {
        this.logicDeleteValue = logicDeleteValue;
    }

    public Map<String, TableColumn> getColumnMap() {
        return columnMap;
    }
    public void setColumnMap(Map<String, TableColumn> columnMap) {
        this.columnMap = columnMap;
    }

    public List<String> getIdKey() {
        return idKey;
    }
    public void setIdKey(List<String> idKey) {
        this.idKey = idKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Table)) return false;
        Table table = (Table) o;
        return Objects.equals(name, table.name) && Objects.equals(desc, table.desc)
                && Objects.equals(alias, table.alias) && Objects.equals(logicColumn, table.logicColumn)
                && Objects.equals(logicValue, table.logicValue)
                && Objects.equals(logicDeleteValue, table.logicDeleteValue)
                && Objects.equals(columnMap, table.columnMap) && Objects.equals(idKey, table.idKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, desc, alias, logicColumn, logicValue, logicDeleteValue, columnMap, idKey);
    }

    @Override
    public String toString() {
        return "Table{" +
                "name='" + name + '\'' +
                ", desc='" + desc + '\'' +
                ", alias='" + alias + '\'' +
                ", logicColumn='" + logicColumn + '\'' +
                ", logicValue='" + logicValue + '\'' +
                ", logicDeleteValue='" + logicDeleteValue + '\'' +
                ", columnMap=" + columnMap +
                ", idKey=" + idKey +
                '}';
    }


    public String idWhere(boolean needAlias) {
        if (idKey.size() == 1) {
            String column = QuerySqlUtil.toSqlField(idKey.get(0));
            if (needAlias) {
                return QuerySqlUtil.toSqlField(alias) + "." + column;
            } else {
                return column;
            }
        } else {
            return "(" + idSelect(needAlias) + ")";
        }
    }
    public String idSelect(boolean needAlias) {
        StringJoiner sj = new StringJoiner(", ");
        for (String id : idKey) {
            String column = QuerySqlUtil.toSqlField(id);
            if (needAlias) {
                sj.add(QuerySqlUtil.toSqlField(alias) + "." + column);
            } else {
                sj.add(column);
            }
        }
        return sj.toString();
    }

    public String generateSelect(Boolean useAlias) {
        StringJoiner sj = new StringJoiner(", ");
        for (TableColumn column : columnMap.values()) {
            if (QueryUtil.isNotNull(useAlias)) {
                sj.add(column.getName() + " AS " + (useAlias ? column.getAlias() : column.getFieldName()));
            } else {
                sj.add(column.getName());
            }
        }
        return sj.toString();
    }


    public String generateInsertMap(Map<String, Object> data, boolean generateNullField,
                                    List<Object> params, StringBuilder printSql) {
        return firstInsertMap(data, generateNullField, new ArrayList<>(), params, printSql);
    }
    private String firstInsertMap(Map<String, Object> data, boolean generateNullField, List<String> placeholderList,
                                  List<Object> params, StringBuilder printSql) {
        StringJoiner sj = new StringJoiner(", ");
        List<String> printList = new ArrayList<>();
        for (TableColumn column : columnMap.values()) {
            Object obj = data.get(column.getAlias());
            if (QueryUtil.isNotNull(obj) || generateNullField) {
                sj.add(QuerySqlUtil.toSqlField(column.getName()));
                placeholderList.add("?");
                printList.add(QuerySqlUtil.toPrintValue(column.getFieldType(), obj));
                params.add(obj);
            }
        }
        if (sj.length() == 0) {
            return "";
        }
        String table = QuerySqlUtil.toSqlField(name);
        String values = String.join(", ", placeholderList);
        String print = String.join(", ", printList);
        printSql.append("INSERT INTO ").append(table).append("(").append(sj).append(") VALUES (").append(print).append(")");
        return "INSERT INTO " + table + "(" + sj + ") VALUES (" + values + ")";
    }
    public String generateBatchInsertMap(List<Map<String, Object>> list, boolean generateNullField,
                                         List<Object> params, StringBuilder printSql) {
        Map<String, Object> first = QueryUtil.first(list);
        List<String> placeholderList = new ArrayList<>();
        String sql = firstInsertMap(first, generateNullField, placeholderList, params, printSql);
        if (QueryUtil.isEmpty(sql)) {
            return "";
        }

        StringJoiner sj = new StringJoiner(", ");
        StringJoiner print = new StringJoiner(", ");
        if (list.size() > 1) {
            List<String> errorList = new ArrayList<>();
            int ps = placeholderList.size();
            for (int i = 1; i < list.size(); i++) {
                Map<String, Object> data = list.get(i);
                List<String> values = new ArrayList<>();
                List<String> printList = new ArrayList<>();
                for (TableColumn column : columnMap.values()) {
                    Object obj = data.get(column.getAlias());
                    if (QueryUtil.isNotNull(obj) || generateNullField) {
                        values.add("?");
                        printList.add(QuerySqlUtil.toPrintValue(column.getFieldType(), obj));
                        params.add(obj);
                    }
                }
                int vs = values.size();
                if (vs != ps) {
                    errorList.add((i + 1) + " : " + vs);
                }
                if (!values.isEmpty()) {
                    sj.add("(" + String.join(", ", values) + ")");
                    print.add("(" + String.join(", ", printList) + ")");
                }
            }
            if (!errorList.isEmpty()) {
                throw new RuntimeException("field number error. 1 : " + ps + " but " + errorList);
            }
        }
        if (sj.length() == 0) {
            return sql;
        } else {
            printSql.append(sql).append(", ").append(print);
            return sql + ", " + sj;
        }
    }

    public <T> String generateInsert(T obj, boolean generateNullField, List<Object> params, StringBuilder printSql) {
        return firstInsert(obj, obj.getClass(), generateNullField, new ArrayList<>(), params, printSql);
    }
    private <T> String firstInsert(T obj, Class<?> clazz, boolean generateNullField,
                                   List<String> placeholderList, List<Object> params, StringBuilder printSql) {
        List<String> fieldList = new ArrayList<>();
        List<String> printList = new ArrayList<>();
        List<String> errorList = new ArrayList<>();
        for (TableColumn column : columnMap.values()) {
            String fieldName = column.getFieldName();
            try {
                Object fieldData = QueryUtil.getFieldData(clazz, fieldName, obj);
                if (QueryUtil.isNotNull(fieldData) || generateNullField) {
                    fieldList.add(QuerySqlUtil.toSqlField(column.getName()));
                    placeholderList.add("?");
                    printList.add(QuerySqlUtil.toPrintValue(column.getFieldType(), fieldData));
                    params.add(fieldData);
                }
            } catch (IllegalAccessException e) {
                errorList.add(fieldName);
            }
        }
        if (QueryUtil.isNotEmpty(errorList)) {
            throw new RuntimeException("get field" + errorList + " data error");
        }
        if (fieldList.isEmpty()) {
            return null;
        }
        String table = QuerySqlUtil.toSqlField(name);
        String fields = String.join(", ", fieldList);
        String values = String.join(", ", placeholderList);
        String print = String.join(", ", printList);
        printSql.append("INSERT INTO ").append(table).append("(").append(fields).append(") VALUES (").append(print).append(")");
        return "INSERT INTO " + table + "(" + fields + ") VALUES (" + values + ")";
    }
    public <T> String generateBatchInsert(List<T> list, boolean generateNullField, List<Object> params, StringBuilder printSql) {
        T first = QueryUtil.first(list);
        Class<?> clazz = first.getClass();
        List<String> placeholderList = new ArrayList<>();
        String sql = firstInsert(first, clazz, generateNullField, placeholderList, params, printSql);
        if (QueryUtil.isEmpty(sql)) {
            return null;
        }

        StringJoiner sj = new StringJoiner(", ");
        if (list.size() > 1) {
            Map<Integer, List<String>> errorMap = new LinkedHashMap<>();
            List<String> countErrorList = new ArrayList<>();
            int ps = placeholderList.size();
            for (int i = 1; i < list.size(); i++) {
                int index = i + 1;
                T obj = list.get(i);
                List<String> values = new ArrayList<>();
                for (TableColumn column : columnMap.values()) {
                    String fieldName = column.getFieldName();
                    try {
                        Object fieldData = QueryUtil.getFieldData(clazz, fieldName, obj);
                        if (QueryUtil.isNotNull(fieldData) || generateNullField) {
                            values.add("?");
                            params.add(fieldData);
                        }
                    } catch (IllegalAccessException e) {
                        errorMap.computeIfAbsent(index, (k) -> new ArrayList<>()).add(fieldName);
                    }
                }
                if (QueryUtil.isNotEmpty(errorMap)) {
                    throw new RuntimeException("get field" + errorMap + " data error");
                }
                int vs = values.size();
                if (vs != ps) {
                    countErrorList.add(index + " : " + vs);
                }
                if (!values.isEmpty()) {
                    sj.add("(" + String.join(", ", values) + ")");
                }
            }
            if (!countErrorList.isEmpty()) {
                throw new RuntimeException("field number error. 1 : " + ps + " but " + countErrorList);
            }
        }
        if (sj.length() == 0) {
            return sql;
        } else {
            return sql + ", " + sj;
        }
    }


    public String generateDelete(SingleTableWhere query, TableColumnInfo tcInfo,
                                 List<Object> params, StringBuilder printSql, boolean force) {
        StringBuilder print = new StringBuilder();
        String where = query.generateSql(name, tcInfo, params, print);
        if (QueryUtil.isEmpty(where)) {
            return null;
        }

        String table = QuerySqlUtil.toSqlField(name);
        if (!force) {
            String set = "";
            if (QueryUtil.isNotEmpty(logicColumn)) {
                set = logicColumn + " = " + logicDeleteValue;
            }
            if (QueryUtil.isNotEmpty(set)) {
                printSql.append("UPDATE ").append(table).append(" SET ").append(set).append(" WHERE ").append(print);
                return "UPDATE " + table + " SET " + set + " WHERE " + where;
            }
        }
        printSql.append("DELETE FROM ").append(table).append(" WHERE ").append(print);
        return "DELETE FROM " + table + " WHERE " + where;
    }


    public String generateCountQuery(SingleTableWhere query, TableColumnInfo tcInfo,
                                     List<Object> params, StringBuilder printSql,
                                     String groupBy, String having, String havingPrint, String orderBy,
                                     List<Integer> pageList, boolean force) {
        return generateQuery(query, tcInfo, params, printSql, "COUNT(*)", groupBy, having, havingPrint, orderBy, pageList, force);
    }
    public String generateQuery(SingleTableWhere query, TableColumnInfo tcInfo,
                                List<Object> params, StringBuilder printSql,
                                String column, String groupBy, String having, String havingPrint, String orderBy,
                                List<Integer> pageList, boolean force) {
        if (QueryUtil.isEmpty(column)) {
            return "";
        }

        StringBuilder wherePrint = new StringBuilder();
        String where = query.generateSql(name, tcInfo, params, wherePrint);
        if (QueryUtil.isEmpty(where)) {
            return "";
        }

        String limit = "";
        StringBuilder limitPrint = new StringBuilder();
        if (QueryUtil.isNotEmpty(pageList)) {
            Integer page = pageList.get(0);
            Integer limitSize = (pageList.size() > 1) ? pageList.get(1) : 0;

            int index = (page == null || page <= 0) ? 1 : page;
            int size = QueryConst.LIMIT_SET.contains(limitSize) ? limitSize : QueryConst.DEFAULT_LIMIT;
            if (index == 1) {
                params.add(size);
                limit = " LIMIT ?";
                limitPrint.append(" LIMIT ").append(size);
            } else {
                params.add((index - 1) * size);
                params.add(size);
                limit = " LIMIT ?, ?";
                limitPrint.append(" LIMIT ").append((index - 1) * size).append(", ").append(size);
            }
        }

        String logicDeleteCondition = (!force && QueryUtil.isNotEmpty(logicColumn) && QueryUtil.isNotEmpty(logicDeleteValue))
                ? (" AND " + logicColumn + " = " + logicDeleteValue) : "";
        // 1. FROM: determine
        // 2. WHERE: filters on the rows
        // 3. GROUP BY: combines those rows into groups
        // 4. HAVING: filters groups
        // 5. ORDER BY: arranges the remaining rows/groups
        // 6. LIMIT: filters on the remaining rows/groups
        String table = QuerySqlUtil.toSqlField(name);
        printSql.append("SELECT ").append(column)
                .append(" FROM ").append(table)
                .append(" WHERE ").append(wherePrint)
                .append(logicDeleteCondition)
                .append(QueryUtil.toStr(groupBy))
                .append(QueryUtil.toStr(havingPrint))
                .append(QueryUtil.toStr(orderBy))
                .append(limitPrint);
        return "SELECT " + column + " FROM " + table + " WHERE " + where + logicDeleteCondition
                + QueryUtil.toStr(groupBy) + QueryUtil.toStr(having) + QueryUtil.toStr(orderBy) + limit;
    }


    public String generateUpdateMap(Map<String, Object> updateObj, boolean generateNullField,
                                    SingleTableWhere query, TableColumnInfo tcInfo,
                                    List<Object> params, StringBuilder printSql) {
        List<String> setList = new ArrayList<>();
        List<String> setPrintList = new ArrayList<>();
        for (TableColumn column : columnMap.values()) {
            Object data = updateObj.get(column.getAlias());
            if (QueryUtil.isNotNull(data) || generateNullField) {
                setList.add(QuerySqlUtil.toSqlField(column.getName()) + " = ?");
                setPrintList.add(QuerySqlUtil.toSqlField(column.getName()) + " = "
                        + QuerySqlUtil.toPrintValue(column.getFieldType(), data));
                params.add(data);
            }
        }
        return update(query, tcInfo, params, printSql, setList, setPrintList);
    }

    public <T> String generateUpdate(T updateObj, boolean generateNullField,
                                     SingleTableWhere query, TableColumnInfo tcInfo,
                                     List<Object> params, StringBuilder printSql) {
        List<String> setList = new ArrayList<>();
        List<String> setPrintList = new ArrayList<>();
        Class<?> clazz = updateObj.getClass();
        for (TableColumn column : columnMap.values()) {
            String fieldName = column.getFieldName();
            Field field = QueryUtil.getField(clazz, fieldName);
            if (QueryUtil.isNotNull(field)) {
                try {
                    field.setAccessible(true);
                    Object fieldInfo = field.get(updateObj);
                    if (QueryUtil.isNotNull(fieldInfo) || generateNullField) {
                        setList.add(QuerySqlUtil.toSqlField(column.getName()) + " = ?");
                        setPrintList.add(QuerySqlUtil.toSqlField(column.getName()) + " = "
                                + QuerySqlUtil.toPrintValue(column.getFieldType(), fieldInfo));
                        params.add(fieldInfo);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(String.format("obj(%s) get field(%s) exception", updateObj, fieldName), e);
                }
            }
        }
        return update(query, tcInfo, params, printSql, setList, setPrintList);
    }

    private String update(SingleTableWhere query, TableColumnInfo tcInfo,
                          List<Object> params, StringBuilder printSql,
                          List<String> setList, List<String> setPrintList) {
        if (setList.isEmpty()) {
            return null;
        }

        StringBuilder print = new StringBuilder();
        String where = query.generateSql(name, tcInfo, params, print);
        if (QueryUtil.isEmpty(where)) {
            return null;
        }

        String set = String.join(", ", setList);
        String setPrint = String.join(", ", setPrintList);
        String table = QuerySqlUtil.toSqlField(name);
        printSql.append("UPDATE ").append(table).append(" SET ").append(setPrint).append(" WHERE ").append(print);
        return "UPDATE " + table + " SET " + set + " WHERE " + where;
    }
}
