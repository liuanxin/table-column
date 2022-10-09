package com.github.liuanxin.query.model;

import java.util.Objects;

public class SchemaColumn {

    /** 表列名 */
    private String name;

    /** 表列说明 */
    private String desc;

    /** 表列别名 */
    private String alias;

    /** true 表示是主键字段 */
    private boolean primary;

    /** 字符串长度 */
    private int strLen;

    /** 表列对应的实体的类型 */
    private Class<?> columnType;

    /** 表列对应的实体的列名 */
    private String fieldName;

    public SchemaColumn() {
    }

    public SchemaColumn(String name, String desc, String alias, boolean primary,
                        int strLen, Class<?> columnType, String fieldName) {
        this.name = name;
        this.desc = desc;
        this.alias = alias;
        this.primary = primary;
        this.strLen = strLen;
        this.columnType = columnType;
        this.fieldName = fieldName;
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

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public int getStrLen() {
        return strLen;
    }

    public void setStrLen(int strLen) {
        this.strLen = strLen;
    }

    public Class<?> getColumnType() {
        return columnType;
    }

    public void setColumnType(Class<?> columnType) {
        this.columnType = columnType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SchemaColumn)) return false;
        SchemaColumn that = (SchemaColumn) o;
        return primary == that.primary && strLen == that.strLen && name.equals(that.name)
                && desc.equals(that.desc) && alias.equals(that.alias)
                && columnType.equals(that.columnType) && fieldName.equals(that.fieldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, desc, alias, primary, strLen, columnType, fieldName);
    }

    @Override
    public String toString() {
        return "SchemaColumn{" +
                "name='" + name + '\'' +
                ", desc='" + desc + '\'' +
                ", alias='" + alias + '\'' +
                ", primary=" + primary +
                ", strLen=" + strLen +
                ", columnType=" + columnType +
                ", fieldName='" + fieldName + '\'' +
                '}';
    }
}
