package com.github.liuanxin.query.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OperateType {

    AND("并且"),
    OR("或者");

    private final String msg;

    OperateType(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    @JsonValue
    public String value() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static OperateType deserializer(Object obj) {
        if (obj != null) {
            String str = obj.toString().trim();
            for (OperateType e : values()) {
                if (str.equalsIgnoreCase(e.name())) {
                    return e;
                }
            }
        }
        return null;
    }
}
