package com.github.liuanxin.query.model;

import java.io.Serializable;
import java.util.Objects;

public class TableColumn implements Serializable {
    private static final long serialVersionUID = 1L;

    /** column name */
    private String name;

    /** column desc */
    private String desc;

    /** column alias */
    private String alias;

    /** true: column has primary key */
    private boolean primary;

    /** varchar type's length */
    private Integer strLen;

    /** true: column has not null */
    private boolean notNull;

    /** true: column has default value */
    private boolean hasDefault;

    private Class<?> fieldType;

    private String fieldName;

    public TableColumn() {}
    public TableColumn(String name, String desc, String alias, boolean primary, Integer strLen,
                       boolean notNull, boolean hasDefault, Class<?> fieldType, String fieldName) {
        this.name = name;
        this.desc = desc;
        this.alias = alias;
        this.primary = primary;
        this.strLen = strLen;
        this.notNull = notNull;
        this.hasDefault = hasDefault;
        this.fieldType = fieldType;
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

    public Integer getStrLen() {
        return strLen;
    }
    public void setStrLen(Integer strLen) {
        this.strLen = strLen;
    }

    public boolean isNotNull() {
        return notNull;
    }
    public void setNotNull(boolean notNull) {
        this.notNull = notNull;
    }

    public boolean isHasDefault() {
        return hasDefault;
    }
    public void setHasDefault(boolean hasDefault) {
        this.hasDefault = hasDefault;
    }

    public Class<?> getFieldType() {
        return fieldType;
    }
    public void setFieldType(Class<?> fieldType) {
        this.fieldType = fieldType;
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
        if (o == null || getClass() != o.getClass()) return false;
        TableColumn that = (TableColumn) o;
        return primary == that.primary && notNull == that.notNull
                && hasDefault == that.hasDefault && Objects.equals(name, that.name)
                && Objects.equals(desc, that.desc) && Objects.equals(alias, that.alias)
                && Objects.equals(strLen, that.strLen) && Objects.equals(fieldType, that.fieldType)
                && Objects.equals(fieldName, that.fieldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, desc, alias, primary, strLen, notNull, hasDefault, fieldType, fieldName);
    }

    @Override
    public String toString() {
        return "TableColumn{" +
                "name='" + name + '\'' +
                ", desc='" + desc + '\'' +
                ", alias='" + alias + '\'' +
                ", primary=" + primary +
                ", strLen=" + strLen +
                ", notNull=" + notNull +
                ", hasDefault=" + hasDefault +
                ", fieldType=" + fieldType +
                ", fieldName='" + fieldName + '\'' +
                '}';
    }
}
