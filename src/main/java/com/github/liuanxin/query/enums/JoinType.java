package com.github.liuanxin.query.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum JoinType {

    INNER("INNER JOIN", "内联"),
    LEFT("LEFT JOIN", "左联"),
    RIGHT("RIGHT JOIN", "右联");

    private final String value;
    private final String msg;

    JoinType(String value, String msg) {
        this.value = value;
        this.msg = msg;
    }

    public String getValue() {
        return value;
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
