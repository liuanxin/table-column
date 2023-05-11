package com.github.liuanxin.query.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.liuanxin.query.util.QueryUtil;

public enum QueryOrder {

    ASC("a"),
    DESC("d");

    private final String value;
    QueryOrder(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }

    @JsonValue
    public String value() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static QueryOrder deserializer(Object obj) {
        if (obj != null) {
            String str = obj.toString().trim();
            for (QueryOrder e : values()) {
                if (str.equalsIgnoreCase(e.name())) {
                    return e;
                }
                if (str.equalsIgnoreCase(e.value)) {
                    return e;
                }
            }
        }
        return null;
    }
    public static String toSql(String order) {
        QueryOrder des = deserializer(order);
        return (QueryUtil.isNull(des) || des == ASC) ? (" " + ASC.name()) : (" " + DESC.name());
    }
}
