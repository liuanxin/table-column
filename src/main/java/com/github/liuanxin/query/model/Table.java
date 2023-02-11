package com.github.liuanxin.query.model;

import com.github.liuanxin.query.util.QuerySqlUtil;
import com.github.liuanxin.query.util.QueryUtil;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;

@SuppressWarnings("DuplicatedCode")
public class Table implements Serializable {
    private static final long serialVersionUID = 1L;

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
    public Table(String name, String desc, String alias, String logicColumn, String logicValue,
                 String logicDeleteValue, Map<String, TableColumn> columnMap) {
        this.name = name;
        this.desc = desc;
        this.alias = alias;
        this.logicColumn = logicColumn;
        this.logicValue = logicValue;
        this.logicDeleteValue = logicDeleteValue;
        this.columnMap = columnMap;

        List<String> idKey = new ArrayList<>();
        if (QueryUtil.isNotEmpty(columnMap)) {
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
        if (o == null || getClass() != o.getClass()) return false;
        Table table = (Table) o;
        return Objects.equals(name, table.name) && Objects.equals(desc, table.desc)
                && Objects.equals(alias, table.alias) && Objects.equals(logicColumn, table.logicColumn)
                && Objects.equals(logicValue, table.logicValue) && Objects.equals(logicDeleteValue, table.logicDeleteValue)
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
            return "( " + idSelect(needAlias) + " )";
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

    public String generateSelect(boolean useAlias, boolean force) {
        StringJoiner sj = new StringJoiner(", ");
        for (TableColumn column : columnMap.values()) {
            String columnName = column.getName();
            if (!columnName.equals(logicColumn) || force) {
                String alias = useAlias ? column.getAlias() : column.getFieldName();
                if (columnName.equals(alias)) {
                    sj.add(columnName);
                } else {
                    sj.add(columnName + " AS " + alias);
                }
            }
        }
        return sj.toString();
    }

    public List<String> allColumn(boolean force) {
        List<String> columnList = new ArrayList<>();
        for (TableColumn column : columnMap.values()) {
            String columnName = column.getName();
            if (!columnName.equals(logicColumn) || force) {
                String fieldName = column.getFieldName();
                if (columnName.equals(fieldName)) {
                    columnList.add(columnName);
                } else {
                    columnList.add(columnName + " AS " + fieldName);
                }
            }
        }
        return columnList;
    }

    public List<String> allColumnAlias(boolean force) {
        List<String> columnInfoList = new ArrayList<>();
        for (TableColumn column : columnMap.values()) {
            String columnName = column.getName();
            if (!columnName.equals(logicColumn) || force) {
                columnInfoList.add(column.getAlias());
            }
        }
        return columnInfoList;
    }



    public String generateInsertMap(Map<String, Object> data, boolean generateNullField,
                                    List<Object> params, StringBuilder printSql) {
        return firstInsertMap(data, generateNullField, new ArrayList<>(), params, printSql);
    }
    private String firstInsertMap(Map<String, Object> data, boolean generateNullField, List<String> placeholderList,
                                  List<Object> params, StringBuilder printSql) {
        StringJoiner sj = new StringJoiner(", ");
        Map<String, String> charLengthMap = new LinkedHashMap<>();
        List<String> printList = new ArrayList<>();
        for (TableColumn column : columnMap.values()) {
            Object obj = data.get(column.getAlias());
            if (QueryUtil.isNotNull(obj) || generateNullField) {
                if (column.getFieldType() == String.class) {
                    int dataLen = QueryUtil.toString(obj).length();
                    int charLen = QueryUtil.toInt(column.getStrLen());
                    if (charLen > 0 && dataLen > charLen) {
                        charLengthMap.put(column.getAlias(), String.format("max(%s) current(%s)", charLen, dataLen));
                    }
                }
                sj.add(QuerySqlUtil.toSqlField(column.getName()));
                placeholderList.add("?");
                printList.add(QuerySqlUtil.toPrintValue(column.getFieldType(), obj));
                params.add(obj);
            }
        }
        if (QueryUtil.isNotEmpty(charLengthMap)) {
            throw new RuntimeException(String.format("table(%s) data length error -> %s", alias, QueryUtil.toStr(charLengthMap)));
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
        if (QueryUtil.isEmpty(first)) {
            return "";
        }

        List<String> placeholderList = new ArrayList<>();
        String sql = firstInsertMap(first, generateNullField, placeholderList, params, printSql);
        if (QueryUtil.isEmpty(sql)) {
            return "";
        }

        StringJoiner sj = new StringJoiner(", ");
        StringJoiner print = new StringJoiner(", ");
        if (list.size() > 1) {
            Map<Integer, Map<String, String>> dataLengthMap = new LinkedHashMap<>();
            List<String> errorList = new ArrayList<>();
            int ps = placeholderList.size();
            for (int i = 1; i < list.size(); i++) {
                int index = i + 1;
                Map<String, Object> data = list.get(i);
                List<String> values = new ArrayList<>();
                List<String> printList = new ArrayList<>();
                for (TableColumn column : columnMap.values()) {
                    Object obj = data.get(column.getAlias());
                    if (QueryUtil.isNotNull(obj) || generateNullField) {
                        if (column.getFieldType() == String.class) {
                            int dataLen = QueryUtil.toString(obj).length();
                            int charLen = QueryUtil.toInt(column.getStrLen());
                            if (charLen > 0 && dataLen > charLen) {
                                String msg = String.format("column(%s) max(%s) current(%s)", column.getName(), charLen, dataLen);
                                dataLengthMap.computeIfAbsent(index, (k) -> new LinkedHashMap<>()).put(column.getAlias(), msg);
                            }
                        }
                        values.add("?");
                        printList.add(QuerySqlUtil.toPrintValue(column.getFieldType(), obj));
                        params.add(obj);
                    }
                }
                int vs = values.size();
                if (vs != ps) {
                    errorList.add(index + " : " + vs);
                }
                if (QueryUtil.isNotEmpty(values)) {
                    sj.add("( " + String.join(", ", values) + " )");
                    print.add("( " + String.join(", ", printList) + " )");
                }
            }
            if (QueryUtil.isNotEmpty(dataLengthMap)) {
                throw new RuntimeException(String.format("table(%s) data length error -> %s", alias, QueryUtil.toStr(dataLengthMap)));
            }
            if (QueryUtil.isNotEmpty(errorList)) {
                throw new RuntimeException("field number error. 1 : " + ps + " but " + QueryUtil.toStr(errorList));
            }
        }
        if (sj.length() == 0) {
            return sql;
        } else {
            printSql.append(", ").append(print);
            return sql + ", " + sj;
        }
    }

    public <T> String generateInsert(T obj, boolean generateNullField, List<Object> params, StringBuilder printSql) {
        return firstInsert(obj, obj.getClass(), generateNullField, new ArrayList<>(), params, printSql);
    }
    private <T> String firstInsert(T obj, Class<?> clazz, boolean generateNullField,
                                   List<String> placeholderList, List<Object> params, StringBuilder printSql) {
        StringJoiner sj = new StringJoiner(", ");
        List<String> printList = new ArrayList<>();
        List<String> errorList = new ArrayList<>();
        Map<String, String> charLengthMap = new LinkedHashMap<>();
        for (TableColumn column : columnMap.values()) {
            String fieldName = column.getFieldName();
            try {
                Object fieldData = QueryUtil.getFieldData(clazz, fieldName, obj);
                if (QueryUtil.isNotNull(fieldData) || generateNullField) {
                    if (column.getFieldType() == String.class) {
                        int dataLen = QueryUtil.toString(fieldData).length();
                        int charLen = QueryUtil.toInt(column.getStrLen());
                        if (charLen > 0 && dataLen > charLen) {
                            charLengthMap.put(column.getAlias(), String.format("column(%s) max(%s) current(%s)", column.getName(), charLen, dataLen));
                        }
                    }
                    sj.add(QuerySqlUtil.toSqlField(column.getName()));
                    placeholderList.add("?");
                    printList.add(QuerySqlUtil.toPrintValue(column.getFieldType(), fieldData));
                    params.add(fieldData);
                }
            } catch (IllegalAccessException e) {
                errorList.add(fieldName);
            }
        }
        if (QueryUtil.isNotEmpty(errorList)) {
            throw new RuntimeException(String.format("table(%s) get field data error -> %s", alias, QueryUtil.toStr(errorList)));
        }
        if (QueryUtil.isNotEmpty(charLengthMap)) {
            throw new RuntimeException(String.format("table(%s) data length error -> %s", alias, QueryUtil.toStr(charLengthMap)));
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
    public <T> String generateBatchInsert(List<T> list, boolean generateNullField, List<Object> params, StringBuilder printSql) {
        T first = QueryUtil.first(list);
        if (QueryUtil.isNull(first)) {
            return "";
        }
        Class<?> clazz = first.getClass();
        List<String> placeholderList = new ArrayList<>();
        String sql = firstInsert(first, clazz, generateNullField, placeholderList, params, printSql);
        if (QueryUtil.isEmpty(sql)) {
            return "";
        }

        StringJoiner sj = new StringJoiner(", ");
        if (list.size() > 1) {
            Map<Integer, List<String>> errorMap = new LinkedHashMap<>();
            Map<Integer, Map<String, String>> dataLengthMap = new LinkedHashMap<>();
            List<String> countErrorList = new ArrayList<>();
            int ps = placeholderList.size();
            for (int i = 1; i < list.size(); i++) {
                T obj = list.get(i);
                List<String> values = new ArrayList<>();
                for (TableColumn column : columnMap.values()) {
                    String fieldName = column.getFieldName();
                    try {
                        Object fieldData = QueryUtil.getFieldData(clazz, fieldName, obj);
                        if (QueryUtil.isNotNull(fieldData) || generateNullField) {
                            if (column.getFieldType() == String.class) {
                                int dataLen = QueryUtil.toString(fieldData).length();
                                int maxLen = QueryUtil.toInt(column.getStrLen());
                                if (maxLen > 0 && dataLen > maxLen) {
                                    String msg = String.format("column(%s) max(%s) current(%s)", column.getName(), maxLen, dataLen);
                                    dataLengthMap.computeIfAbsent(i, (k) -> new LinkedHashMap<>()).put(column.getAlias(), msg);
                                }
                            }
                            values.add("?");
                            params.add(fieldData);
                        }
                    } catch (IllegalAccessException e) {
                        errorMap.computeIfAbsent(i, (k) -> new ArrayList<>()).add(fieldName);
                    }
                }
                int vs = values.size();
                if (vs != ps) {
                    countErrorList.add(i + " : " + vs);
                }
                if (QueryUtil.isNotEmpty(values)) {
                    sj.add("( " + String.join(", ", values) + " )");
                }
            }
            if (QueryUtil.isNotEmpty(errorMap)) {
                throw new RuntimeException(String.format("table(%s) get field data error -> %s", alias, QueryUtil.toStr(errorMap)));
            }
            if (QueryUtil.isNotEmpty(dataLengthMap)) {
                throw new RuntimeException(String.format("table(%s) data length error -> %s", alias, QueryUtil.toStr(dataLengthMap)));
            }
            if (QueryUtil.isNotEmpty(countErrorList)) {
                throw new RuntimeException("field number error. 0 : " + ps + " but " + QueryUtil.toStr(countErrorList));
            }
        }
        if (sj.length() == 0) {
            return sql;
        } else {
            printSql.append(", ").append(sj);
            return sql + ", " + sj;
        }
    }


    public String generateDelete(ReqQuery query, TableColumnInfo tcInfo,
                                 List<Object> params, StringBuilder printSql, boolean force) {
        StringBuilder wherePrint = new StringBuilder();
        String where = query.generateSql(name, tcInfo, false, params, wherePrint);
        if (QueryUtil.isEmpty(where)) {
            return "";
        }

        String table = QuerySqlUtil.toSqlField(name);
        if (!force && QueryUtil.isNotEmpty(logicColumn) && QueryUtil.isNotEmpty(logicDeleteValue)) {
            String logicDelete = logicColumn + " = " + logicDeleteValue;
            String update = "UPDATE " + table + " SET " + logicDelete + " WHERE ";
            printSql.append(update).append(wherePrint);
            return update + where;
        } else {
            printSql.append("DELETE FROM ").append(table).append(" WHERE ").append(wherePrint);
            return "DELETE FROM " + table + " WHERE " + where;
        }
    }


    public String logicDeleteCondition(boolean force, boolean needAlias) {
        if (!force && QueryUtil.isNotEmpty(logicColumn) && QueryUtil.isNotEmpty(logicValue)) {
            String tableAlias = needAlias ? (QuerySqlUtil.toSqlField(alias) + ".") : "";
            String column = QuerySqlUtil.toSqlField(logicColumn);
            return " AND " + tableAlias + column + " = " + logicValue;
        } else {
            return "";
        }
    }


    public String generateUpdateMap(Map<String, Object> updateObj, boolean generateNullField, ReqQuery query,
                                    TableColumnInfo tcInfo, List<Object> params, StringBuilder printSql) {
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

    public <T> String generateUpdate(T updateObj, boolean generateNullField, ReqQuery query,
                                     TableColumnInfo tcInfo, List<Object> params, StringBuilder printSql) {
        List<String> setList = new ArrayList<>();
        List<String> setPrintList = new ArrayList<>();
        Class<?> clazz = updateObj.getClass();
        List<String> errorColumnList = new ArrayList<>();
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
                    errorColumnList.add(fieldName);
                }
            }
        }
        if (QueryUtil.isNotEmpty(errorColumnList)) {
            throw new RuntimeException(String.format("obj(%s) get field(%s) exception", updateObj, errorColumnList));
        }
        return update(query, tcInfo, params, printSql, setList, setPrintList);
    }

    private String update(ReqQuery query, TableColumnInfo tcInfo, List<Object> params,
                          StringBuilder printSql, List<String> setList, List<String> setPrintList) {
        if (QueryUtil.isEmpty(setList)) {
            return "";
        }

        StringBuilder print = new StringBuilder();
        String where = query.generateSql(name, tcInfo, false, params, print);
        if (QueryUtil.isEmpty(where)) {
            return "";
        }

        String set = String.join(", ", setList);
        String setPrint = String.join(", ", setPrintList);
        String table = QuerySqlUtil.toSqlField(name);
        printSql.append("UPDATE ").append(table).append(" SET ").append(setPrint).append(" WHERE ").append(print);
        return "UPDATE " + table + " SET " + set + " WHERE " + where;
    }
}
