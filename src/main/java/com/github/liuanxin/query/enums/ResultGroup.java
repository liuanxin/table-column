package com.github.liuanxin.query.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.liuanxin.query.util.QueryUtil;

public enum ResultGroup {

    COUNT("COUNT(%s)", "总条数"),
    COUNT_DISTINCT("COUNT(DISTINCT %s)", "总条数(去重)"),
    SUM("SUM(%s)", "总和"),
    MIN("MIN(%s)", "最小"),
    MAX("MAX(%s)", "最大"),
    AVG("AVG(%s)", "平均"),
    GROUP_CONCAT("GROUP_CONCAT(%s)", "组拼接");

    private final String value;
    private final String msg;

    ResultGroup(String value, String msg) {
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
    public static ResultGroup deserializer(Object obj) {
        if (obj != null) {
            String str = obj.toString().trim();
            for (ResultGroup e : values()) {
                if (str.equalsIgnoreCase(e.name())) {
                    return e;
                }
            }
        }
        return null;
    }

    public String generateColumn(String column) {
        return String.format(value, column);
    }

    public boolean checkNotHavingValue(Object value) {
        if (this == COUNT || this == COUNT_DISTINCT) {
            // COUNT  SUM  function needs to be int
            return QueryUtil.isNotLong(value);
        } else if (this == SUM || this == MIN || this == MAX || this == AVG) {
            // MIN  MAX  AVG  function needs to be double
            return QueryUtil.isNotDouble(value);
        } else {
            return false;
        }
    }
}
