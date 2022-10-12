package com.github.liuanxin.query.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.liuanxin.query.util.QueryUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum ResultGroup {

    COUNT("COUNT(%s)", "CNT%s", "总条数"),
    COUNT_DISTINCT("COUNT(DISTINCT %s)", "CNT_DIS_%s", "总条数(去重)"),
    SUM("SUM(%s)", "SUM_%S", "总和"),
    MIN("MIN(%s)", "MIN_%s", "最小"),
    MAX("MAX(%s)", "MAX_%s", "最大"),
    AVG("AVG(%s)", "AVG_%s", "平均"),
    GROUP_CONCAT("GROUP_CONCAT(%s)", "GPCT_%s", "组拼接");

    public static final Set<String> SUPPORT_COUNT_SET = new HashSet<>(Arrays.asList("*", "1"));

    private final String value;
    private final String alias;
    private final String msg;

    ResultGroup(String value, String alias, String msg) {
        this.value = value;
        this.alias = alias;
        this.msg = msg;
    }

    public String getValue() {
        return value;
    }

    public String getAlias() {
        return alias;
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
        return String.format(value, column) + " AS " + generateAlias(column);
    }
    public String generateAlias(String column) {
        return String.format(alias, SUPPORT_COUNT_SET.contains(column) ? "" : column.replace(" ", "$").replace(",", "_"));
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
