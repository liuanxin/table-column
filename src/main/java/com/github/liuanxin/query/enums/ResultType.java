package com.github.liuanxin.query.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ResultType {

    OBJ("对象(键值对映射)"),
    ARR("数组");

    private final String msg;

    ResultType(String msg) {
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
    public static ResultType deserializer(Object obj) {
        if (obj != null) {
            String str = obj.toString().trim();
            for (ResultType e : values()) {
                if (str.equalsIgnoreCase(e.name())) {
                    return e;
                }
            }
        }
        return null;
    }
}
