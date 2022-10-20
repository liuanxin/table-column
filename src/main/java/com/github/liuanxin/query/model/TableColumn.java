package com.github.liuanxin.query.model;

import java.util.Objects;

public class TableColumn {

    /** 表列名 */
    private String name;

    /** 表列说明 */
    private String desc;

    /** 表列别名 */
    private String alias;

    /** true 表示是主键字段 */
    private boolean primary;

    /** 字符串长度 */
    private Integer strLen;

    /** 表列对应的实体的类型 */
    private Class<?> fieldType;

    /** 表列对应的实体的列名 */
    private String fieldName;

    public TableColumn() {}
    public TableColumn(String name, String desc, String alias, boolean primary,
                       Integer strLen, Class<?> fieldType, String fieldName) {
        this.name = name;
        this.desc = desc;
        this.alias = alias;
        this.primary = primary;
        this.strLen = strLen;
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
        if (!(o instanceof TableColumn)) return false;
        TableColumn that = (TableColumn) o;
        return primary == that.primary && Objects.equals(name, that.name)
                && Objects.equals(desc, that.desc) && Objects.equals(alias, that.alias)
                && Objects.equals(strLen, that.strLen) && Objects.equals(fieldType, that.fieldType)
                && Objects.equals(fieldName, that.fieldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, desc, alias, primary, strLen, fieldType, fieldName);
    }

    @Override
    public String toString() {
        return "TableColumn{" +
                "name='" + name + '\'' +
                ", desc='" + desc + '\'' +
                ", alias='" + alias + '\'' +
                ", primary=" + primary +
                ", strLen=" + strLen +
                ", fieldType=" + fieldType +
                ", fieldName='" + fieldName + '\'' +
                '}';
    }
}
