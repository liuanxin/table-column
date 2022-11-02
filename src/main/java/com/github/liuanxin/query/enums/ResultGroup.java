package com.github.liuanxin.query.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.liuanxin.query.constant.QueryConst;
import com.github.liuanxin.query.util.QueryUtil;

public enum ResultGroup {

    COUNT("COUNT(%s)", "_cnt_%s", "总条数"),
    COUNT_DISTINCT("COUNT(DISTINCT %s)", "_cnt_dis_%s", "去重后的总条数"),
    SUM("SUM(%s)", "_sum_%s", "总和"),
    MIN("MIN(%s)", "_min_%s", "最小"),
    MAX("MAX(%s)", "_max_%s", "最大"),
    AVG("AVG(%s)", "_avg_%s", "平均"),
    GROUP_CONCAT("GROUP_CONCAT(%s)", "_gpct_%s", "组拼接");

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
        String args;
        if (needCheckColumn(column)) {
            args = column.replace(" ", "_s_").replace(".", "_r_").replace(",", "_");
        } else {
            args = "";
        }
        return String.format(alias, args);
    }

    public boolean needCheckColumn(String column) {
        return !(this == COUNT && QueryConst.SUPPORT_COUNT_SET.contains(column));
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
