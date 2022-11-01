package com.github.liuanxin.query.enums;

public enum TableRelationType {

    ONE_TO_ONE("一对一"),
    ONE_TO_MANY("一对多");
    // 不支持多对多

    private final String msg;
    TableRelationType(String msg) {
        this.msg = msg;
    }
    public String getMsg() {
        return msg;
    }

    public boolean hasMany() {
        return this != ONE_TO_ONE;
    }
}
