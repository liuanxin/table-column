package com.github.liuanxin.query.model;

import com.github.liuanxin.query.util.QuerySqlUtil;
import com.github.liuanxin.query.util.QueryUtil;

import java.lang.reflect.Field;
import java.util.*;

public class Table {

    /** 表名 */
    private String name;

    /** 表说明 */
    private String desc;

    /** 表别名 */
    private String alias;

    /** 列信息 */
    private Map<String, TableColumn> columnMap;

    /** 主键列 */
    private List<String> idKey;

    public Table() {}
    public Table(String name, String desc, String alias, Map<String, TableColumn> columnMap) {
        this.name = name;
        this.desc = desc;
        this.alias = alias;
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
                && Objects.equals(alias, table.alias) && Objects.equals(columnMap, table.columnMap)
                && Objects.equals(idKey, table.idKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, desc, alias, columnMap, idKey);
    }

    @Override
    public String toString() {
        return "Table{" +
                "name='" + name + '\'' +
                ", desc='" + desc + '\'' +
                ", alias='" + alias + '\'' +
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


    public String generateInsertMap(Map<String, Object> data, boolean generateNullField, List<Object> params) {
        return firstInsertMap(data, generateNullField, new ArrayList<>(), params);
    }
    private String firstInsertMap(Map<String, Object> data, boolean generateNullField,
                                  List<String> placeholderList, List<Object> params) {
        StringJoiner sj = new StringJoiner(", ");
        for (TableColumn column : columnMap.values()) {
            Object obj = data.get(column.getAlias());
            if (QueryUtil.isNotNull(obj) || generateNullField) {
                sj.add(QuerySqlUtil.toSqlField(column.getName()));
                placeholderList.add("?");
                params.add(obj);
            }
        }
        if (sj.length() == 0) {
            return null;
        }
        String table = QuerySqlUtil.toSqlField(name);
        String values = String.join(", ", placeholderList);
        return "INSERT INTO " + table + "(" + sj + ") VALUES (" + values + ")";
    }
    public String generateBatchInsertMap(List<Map<String, Object>> list, boolean generateNullField, List<Object> params) {
        Map<String, Object> first = QueryUtil.first(list);
        List<String> placeholderList = new ArrayList<>();
        String sql = firstInsertMap(first, generateNullField, placeholderList, params);
        if (QueryUtil.isEmpty(sql)) {
            return null;
        }

        StringJoiner sj = new StringJoiner(", ");
        if (list.size() > 1) {
            List<String> errorList = new ArrayList<>();
            int ps = placeholderList.size();
            for (int i = 1; i < list.size(); i++) {
                Map<String, Object> data = list.get(i);
                List<String> values = new ArrayList<>();
                for (TableColumn column : columnMap.values()) {
                    Object obj = data.get(column.getAlias());
                    if (QueryUtil.isNotNull(obj) || generateNullField) {
                        values.add("?");
                        params.add(obj);
                    }
                }
                int vs = values.size();
                if (vs != ps) {
                    errorList.add((i + 1) + " : " + vs);
                }
                if (!values.isEmpty()) {
                    sj.add("(" + String.join(", ", values) + ")");
                }
            }
            if (!errorList.isEmpty()) {
                throw new RuntimeException("field number error. 1 : " + ps + " but " + errorList);
            }
        }
        return (sj.length() == 0) ? sql : (sql + ", " + sj);
    }

    public <T> String generateInsert(T obj, boolean generateNullField, List<Object> params) {
        return firstInsert(obj, obj.getClass(), generateNullField, new ArrayList<>(), params);
    }
    private <T> String firstInsert(T obj, Class<?> clazz, boolean generateNullField,
                                   List<String> placeholderList, List<Object> params) {
        List<String> fieldList = new ArrayList<>();
        for (TableColumn column : columnMap.values()) {
            String fieldName = column.getFieldName();
            Field field = QueryUtil.getField(clazz, fieldName);
            if (QueryUtil.isNotNull(field)) {
                try {
                    field.setAccessible(true);
                    Object fieldInfo = field.get(obj);
                    if (QueryUtil.isNotNull(fieldInfo) || generateNullField) {
                        fieldList.add(QuerySqlUtil.toSqlField(column.getName()));
                        placeholderList.add("?");
                        params.add(fieldInfo);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(String.format("obj(%s) get field(%s) exception", obj, fieldName), e);
                }
            }
        }
        if (fieldList.isEmpty()) {
            return null;
        }
        String table = QuerySqlUtil.toSqlField(name);
        String fields = String.join(", ", fieldList);
        String values = String.join(", ", placeholderList);
        return "INSERT INTO " + table + "(" + fields + ") VALUES (" + values + ")";
    }
    public <T> String generateBatchInsert(List<T> list, boolean generateNullField, List<Object> params) {
        T first = QueryUtil.first(list);
        Class<?> clazz = first.getClass();
        List<String> placeholderList = new ArrayList<>();
        String sql = firstInsert(first, clazz, generateNullField, placeholderList, params);
        if (QueryUtil.isEmpty(sql)) {
            return null;
        }

        StringJoiner sj = new StringJoiner(", ");
        if (list.size() > 1) {
            List<String> errorList = new ArrayList<>();
            int ps = placeholderList.size();
            for (int i = 1; i < list.size(); i++) {
                T obj = list.get(i);
                List<String> values = new ArrayList<>();
                for (TableColumn column : columnMap.values()) {
                    String fieldName = column.getFieldName();
                    Field field = QueryUtil.getField(clazz, fieldName);
                    if (QueryUtil.isNotNull(field)) {
                        try {
                            field.setAccessible(true);
                            Object fieldInfo = field.get(obj);
                            if (QueryUtil.isNotNull(fieldInfo) || generateNullField) {
                                values.add("?");
                                params.add(fieldInfo);
                            }
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(String.format("index(%s) obj(%s) get field(%s) exception",
                                    (i + 1), obj, fieldName), e);
                        }
                    }
                }
                int vs = values.size();
                if (vs != ps) {
                    errorList.add((i + 1) + " : " + vs);
                }
                if (!values.isEmpty()) {
                    sj.add("(" + String.join(", ", values) + ")");
                }
            }
            if (!errorList.isEmpty()) {
                throw new RuntimeException("field number error. 1 : " + ps + " but " + errorList);
            }
        }
        return (sj.length() == 0) ? sql : (sql + ", " + sj);
    }


    public String generateDelete(SingleTableWhere query, TableColumnInfo scInfo, List<Object> params) {
        String where = query.generateSql(name, scInfo, params);
        if (QueryUtil.isEmpty(where)) {
            return null;
        }

        String table = QuerySqlUtil.toSqlField(name);
        return "DELETE FROM " + table + " WHERE " + where;
    }


    public String generateUpdateMap(Map<String, Object> updateObj, boolean generateNullField,
                                    SingleTableWhere query, TableColumnInfo scInfo, List<Object> params) {
        List<String> setList = new ArrayList<>();
        for (TableColumn column : columnMap.values()) {
            Object data = updateObj.get(column.getAlias());
            if (QueryUtil.isNotNull(data) || generateNullField) {
                setList.add(QuerySqlUtil.toSqlField(column.getName()) + " = ?");
                params.add(data);
            }
        }
        return update(query, scInfo, params, setList);
    }

    public <T> String generateUpdate(T updateObj, boolean generateNullField,
                                     SingleTableWhere query, TableColumnInfo scInfo, List<Object> params) {
        List<String> setList = new ArrayList<>();
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
                        params.add(fieldInfo);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(String.format("obj(%s) get field(%s) exception", updateObj, fieldName), e);
                }
            }
        }
        return update(query, scInfo, params, setList);
    }

    private String update(SingleTableWhere query, TableColumnInfo scInfo, List<Object> params, List<String> setList) {
        if (setList.isEmpty()) {
            return null;
        }

        String where = query.generateSql(name, scInfo, params);
        if (QueryUtil.isEmpty(where)) {
            return null;
        }

        String set = String.join(", ", setList);
        String table = QuerySqlUtil.toSqlField(name);
        return "UPDATE " + table + " SET " + set + " WHERE " + where;
    }
}
