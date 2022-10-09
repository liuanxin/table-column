package com.github.liuanxin.query.model;

import com.github.liuanxin.query.util.QuerySqlUtil;
import com.github.liuanxin.query.util.QueryUtil;

import java.lang.reflect.Field;
import java.util.*;

public class Schema {

    /** 表名 */
    private String name;

    /** 表说明 */
    private String desc;

    /** 表别名 */
    private String alias;

    /** 列信息 */
    private Map<String, SchemaColumn> columnMap;

    /** 主键列 */
    private List<String> idKey;

    public Schema() {
    }

    public Schema(String name, String desc, String alias, Map<String, SchemaColumn> columnMap) {
        this.name = name;
        this.desc = desc;
        this.alias = alias;
        this.columnMap = columnMap;

        List<String> idKey = new ArrayList<>();
        if (!columnMap.isEmpty()) {
            for (SchemaColumn schemaColumn : columnMap.values()) {
                if (schemaColumn.isPrimary()) {
                    idKey.add(schemaColumn.getName());
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

    public Map<String, SchemaColumn> getColumnMap() {
        return columnMap;
    }

    public void setColumnMap(Map<String, SchemaColumn> columnMap) {
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
        if (!(o instanceof Schema)) return false;
        Schema schema = (Schema) o;
        return Objects.equals(name, schema.name) && Objects.equals(desc, schema.desc)
                && Objects.equals(alias, schema.alias) && Objects.equals(columnMap, schema.columnMap)
                && Objects.equals(idKey, schema.idKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, desc, alias, columnMap, idKey);
    }

    @Override
    public String toString() {
        return "Schema{" +
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


    public <T> String generateInsert(T obj, boolean generateNullField, List<Object> params) {
        return firstInsert(obj, generateNullField, new ArrayList<>(), params);
    }
    private <T> String firstInsert(T obj, boolean generateNullField, List<String> placeholderList, List<Object> params) {
        Class<?> clazz = obj.getClass();
        List<String> fieldList = new ArrayList<>();
        for (SchemaColumn column : columnMap.values()) {
            String fieldName = column.getFieldName();
            Field field = QueryUtil.getField(clazz, fieldName);
            if (field != null) {
                try {
                    field.setAccessible(true);
                    Object fieldInfo = field.get(obj);
                    if (fieldInfo != null || generateNullField) {
                        fieldList.add(QuerySqlUtil.toSqlField(column.getName()));
                        placeholderList.add("?");
                        params.add(fieldInfo);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(String.format("obj(%s) get field(%s) exception", obj, fieldName), e);
                }
            }
        }
        if (fieldList.isEmpty() || placeholderList.isEmpty()) {
            return null;
        }
        String schema = QuerySqlUtil.toSqlField(name);
        String fields = String.join(", ", fieldList);
        String values = String.join(", ", placeholderList);
        return "INSERT INTO " + schema + "(" + fields + ") VALUES (" + values + ")";
    }
    public <T> String generateBatchInsert(List<T> list, boolean generateNullField, List<Object> params) {
        T first = QueryUtil.first(list);
        List<String> placeholderList = new ArrayList<>();
        String sql = firstInsert(first, generateNullField, placeholderList, params);
        if (placeholderList.isEmpty() || sql == null) {
            return null;
        }
        List<String> valueList = new ArrayList<>();
        if (list.size() > 1) {
            Class<?> clazz = first.getClass();
            for (int i = 1; i < list.size(); i++) {
                T obj = list.get(i);
                List<String> values = new ArrayList<>();
                for (SchemaColumn column : columnMap.values()) {
                    String fieldName = column.getFieldName();
                    Field field = QueryUtil.getField(clazz, fieldName);
                    if (field != null) {
                        try {
                            field.setAccessible(true);
                            Object fieldInfo = field.get(obj);
                            if (fieldInfo != null || generateNullField) {
                                values.add("?");
                                params.add(fieldInfo);
                            }
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(String.format("obj(%s) get field(%s) exception", obj, fieldName), e);
                        }
                    }
                }
                int vs = values.size();
                int ps = placeholderList.size();
                if (vs != ps) {
                    throw new RuntimeException("Collection index(" + i + ") fields number(" + vs + ") != first number(" + ps + ")");
                }
                if (!values.isEmpty()) {
                    valueList.add("(" + String.join(", ", values) + ")");
                }
            }
        }
        return sql + (valueList.isEmpty() ? "" : ("," + String.join(", ", valueList)));
    }

    public String generateDelete(SingleSchemaWhere query, SchemaColumnInfo scInfo, List<Object> params) {
        String where = query.generateSql(name, scInfo, params);
        if (where == null || where.isEmpty()) {
            return null;
        }

        String schema = QuerySqlUtil.toSqlField(name);
        return "DELETE FROM " + schema + " WHERE " + where;
    }

    public <T> String generateUpdate(T updateObj, boolean generateNullField, SingleSchemaWhere query,
                                     SchemaColumnInfo scInfo, List<Object> params) {
        List<String> setList = new ArrayList<>();
        Class<?> clazz = updateObj.getClass();
        for (SchemaColumn column : columnMap.values()) {
            String fieldName = column.getFieldName();
            Field field = QueryUtil.getField(clazz, fieldName);
            if (field != null) {
                try {
                    field.setAccessible(true);
                    Object fieldInfo = field.get(updateObj);
                    if (fieldInfo != null || generateNullField) {
                        setList.add(QuerySqlUtil.toSqlField(column.getName()) + " = ?");
                        params.add(fieldInfo);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(String.format("obj(%s) get field(%s) exception", updateObj, fieldName), e);
                }
            }
        }
        if (setList.isEmpty()) {
            return null;
        }
        String where = query.generateSql(name, scInfo, params);
        if (where == null || where.isEmpty()) {
            return null;
        }

        String set = String.join(", ", setList);
        String schema = QuerySqlUtil.toSqlField(name);
        return "UPDATE " + schema + " SET " + set + " WHERE " + where;
    }
}
