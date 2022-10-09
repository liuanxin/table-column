package com.github.liuanxin.query.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum JoinType {

    INNER("内联"),
    LEFT("左联"),
    RIGHT("右联");

    private final String msg;

    JoinType(String msg) {
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
    public static JoinType deserializer(Object obj) {
        if (obj != null) {
            String str = obj.toString().trim();
            for (JoinType e : values()) {
                if (str.equalsIgnoreCase(e.name())) {
                    return e;
                }
            }
        }
        return null;
    }
}
