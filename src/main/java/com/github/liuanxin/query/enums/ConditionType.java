package com.github.liuanxin.query.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.liuanxin.query.util.QuerySqlUtil;
import com.github.liuanxin.query.util.QueryUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <pre>
 * global:
 *   is null      不常用
 *   is not null  不常用
 *   =
 *   <>
 *
 * list:
 *   in
 *   not in
 *
 * number/date:
 *   >
 *   >=
 *   <
 *   <=
 *   between
 *
 * string:
 *   like
 *   not like     不常用
 * </pre>
 */
public enum ConditionType {

    /*
    eq  : 等于
    ne  : 不等于
    in  : 批量(多个)
    bet : 区间(时间或数字)
    gt  : 大于
    ge  : 大于等于
    lt  : 小于
    le  : 小于等于
    inc : 包含
    sta : 开头
    end : 结尾

    ------------------------------

    eq  : =
    ne  : <>
    in  :
    bet : BETWEEN
    gt  : >
    ge  : >=
    lt  : <
    le  : <=
    inc : LIKE '%x%'
    sta : LIKE 'x%'
    end : LIKE '%x'

    下面几种是不常用的

    nu  : IS NULL        : 为空
    nn  : IS NOT NULL    : 不为空
    ni  : NOT IN         : 不在其中(多个)
    nl  : NOT LIKE '%x%' : 不包含
    nbe : NOT BETWEEN    : 不在区间
    */

    NU("IS NULL", "为空") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return String.format(" %s %s", QuerySqlUtil.toSqlField(column), getValue());
        }
    },
    NN("IS NOT NULL", "不为空") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return String.format(" %s %s", QuerySqlUtil.toSqlField(column), getValue());
        }
    },

    EQ("=", "等于") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateCondition(column, type, value, params);
        }
    },
    NE("<>", "不等于") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateCondition(column, type, value, params);
        }
    },

    IN("IN", "批量") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateMulti(column, type, value, params);
        }
    },
    NI("NOT IN", "不在其中") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateMulti(column, type, value, params);
        }
    },

    BET("BETWEEN", "区间") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateMulti(column, type, value, params);
        }
    },
    NBE("NOT BETWEEN", "不在区间") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateMulti(column, type, value, params);
        }
    },
    GT(">", "大于") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateCondition(column, type, value, params);
        }
    },
    GE(">=", "大于等于") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateCondition(column, type, value, params);
        }
    },
    LT("<", "小于") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateCondition(column, type, value, params);
        }
    },
    LE("<=", "小于等于") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateCondition(column, type, value, params);
        }
    },

    INC("LIKE", "包含") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateCondition(column, type, ("%" + value + "%"), params);
        }
    },
    STA("LIKE", "开头") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateCondition(column, type, (value + "%"), params);
        }
    },
    END("LIKE", "结尾") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateCondition(column, type, ("%" + value), params);
        }
    },
    NL("NOT LIKE", "不包含") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateCondition(column, type, ("%" + value + "%"), params);
        }
    };


    @JsonValue
    private final String value;
    private final String msg;

    ConditionType(String value, String msg) {
        this.value = value;
        this.msg = msg;
    }

    public String getValue() {
        return value;
    }

    public String getMsg() {
        return msg;
    }

    @JsonCreator
    public static ConditionType deserializer(Object obj) {
        if (obj != null) {
            String str = obj.toString().trim();
            for (ConditionType e : values()) {
                if (str.equalsIgnoreCase(e.name())) {
                    return e;
                }
                if (str.length() > 2 && str.toLowerCase().startsWith(e.name().toLowerCase())) {
                    return e;
                }
            }
        }
        return null;
    }


    public abstract String generateSql(String column, Class<?> type, Object value, List<Object> params);


    public String info() {
        return name().toLowerCase() + "(" + msg + ")";
    }

    public void checkTypeAndValue(Class<?> type, String column, Object value, Integer strLen, int maxListCount) {
        checkType(type, column);
        checkValue(type, column, value, strLen, maxListCount);
    }


    protected String generateCondition(String column, Class<?> type, Object value, List<Object> params) {
        if (value == null) {
            return "";
        } else {
            params.add(toValue(type, value));
            return String.format(" %s %s ?", column, getValue());
        }
    }
    protected String generateMulti(String column, Class<?> type, Object value, List<Object> params) {
        if (value == null || !MULTI_TYPE.contains(this) || !(value instanceof Collection<?>)) {
            return "";
        }
        Collection<?> c = (Collection<?>) value;
        if (c.isEmpty()) {
            return "";
        }

        if (this == BET || this == NBE) {
            Object[] arr = c.toArray();
            Object start = arr[0];
            Object end = arr.length > 1 ? arr[1] : null;

            StringBuilder sbd = new StringBuilder();
            if (QueryUtil.isNotNull(start) && QueryUtil.isNotNull(end)) {
                params.add(toValue(type, start));
                params.add(toValue(type, end));
                sbd.append(" ").append(column).append(" BETWEEN ? AND ?");
            } else {
                if (QueryUtil.isNotNull(start)) {
                    params.add(toValue(type, start));
                    sbd.append(" ").append(column).append(" >= ?");
                }
                if (QueryUtil.isNotNull(end)) {
                    params.add(toValue(type, end));
                    sbd.append(" ").append(column).append(" <= ?");
                }
            }
            return sbd.toString();
        } else {
            boolean hasChange = false;
            StringJoiner sj = new StringJoiner(", ");
            for (Object obj : c) {
                if (obj != null) {
                    if (!hasChange) {
                        hasChange = true;
                    }
                    sj.add("?");
                    params.add(toValue(type, obj));
                }
            }
            return hasChange ? String.format(" %s %s (%s)", column, getValue(), sj) : "";
        }
    }


    private static final Set<ConditionType> MULTI_TYPE = new HashSet<>(Arrays.asList(
            IN,
            NI,
            BET,
            NBE
    ));
    /** string: 等于(eq)、不等于(ne)、批量(in)、不在其中(ni)、包含(include)、开头(start)、结尾(end) */
    private static final Set<ConditionType> STRING_TYPE_SET = new LinkedHashSet<>(Arrays.asList(
            EQ,
            NE,
            IN,
            NI,
            INC,
            STA,
            END
    ));
    private static final String STRING_TYPE_INFO = String.format("String type can only be used in 「%s」 conditions",
            STRING_TYPE_SET.stream().map(ConditionType::info).collect(Collectors.joining(", ")));

    /** number: 等于(eq)、不等于(ne)、批量(in)、不在其中(ni)、大于(gt)、大于等于(ge)、小于(lt)、小于等于(le)、区间(bet)、不在区间(nbe) */
    private static final Set<ConditionType> NUMBER_TYPE_SET = new LinkedHashSet<>(Arrays.asList(
            EQ,
            NE,
            IN,
            NI,
            GT,
            GE,
            LT,
            LE,
            BET,
            NBE
    ));
    private static final String NUMBER_TYPE_INFO = String.format("Number type can only be used in 「%s」 conditions",
            NUMBER_TYPE_SET.stream().map(ConditionType::info).collect(Collectors.joining(", ")));

    /** date: 大于(gt)、大于等于(ge)、小于(lt)、小于等于(le)、区间(bet)、不在区间(nbe) */
    private static final Set<ConditionType> DATE_TYPE_SET = new LinkedHashSet<>(Arrays.asList(
            GT,
            GE,
            LT,
            LE,
            BET,
            NBE
    ));
    private static final String DATE_TYPE_INFO = String.format("Date type can only be used in 「%s」 conditions",
            DATE_TYPE_SET.stream().map(ConditionType::info).collect(Collectors.joining(", ")));

    /**  非 string/number/date 类型: 等于(eq)、不等于(ne) */
    private static final Set<ConditionType> OTHER_TYPE_SET = new HashSet<>(Arrays.asList(
            EQ,
            NE
    ));
    private static final String OTHER_TYPE_INFO = String.format("Non(String, Number, Date) type can only be used in 「%s」 conditions",
            OTHER_TYPE_SET.stream().map(ConditionType::info).collect(Collectors.joining(", ")));

    private static final Set<Class<?>> BOOLEAN_TYPE_SET = new HashSet<>(Arrays.asList(Boolean.class, boolean.class));
    private static final Set<Class<?>> INT_TYPE_SET = new HashSet<>(Arrays.asList(Integer.class, int.class));
    private static final Set<Class<?>> LONG_TYPE_SET = new HashSet<>(Arrays.asList(Long.class, long.class));


    private void checkType(Class<?> type, String column) {
        if (Number.class.isAssignableFrom(type)) {
            if (!NUMBER_TYPE_SET.contains(this)) {
                throw new RuntimeException(column + ": " + NUMBER_TYPE_INFO);
            }
        } else if (Date.class.isAssignableFrom(type)) {
            if (!DATE_TYPE_SET.contains(this)) {
                throw new RuntimeException(column + ": " + DATE_TYPE_INFO);
            }
        } else if (String.class.isAssignableFrom(type)) {
            if (!STRING_TYPE_SET.contains(this)) {
                throw new RuntimeException(column + ": " + STRING_TYPE_INFO);
            }
        } else {
            if (!OTHER_TYPE_SET.contains(this)) {
                throw new RuntimeException(column + ": " + OTHER_TYPE_INFO);
            }
        }
    }

    private void checkValue(Class<?> type, String column, Object value, Integer strLen, int maxListCount) {
        if (value != null) {
            if (MULTI_TYPE.contains(this)) {
                if (value instanceof Collection<?>) {
                    int count = 0;
                    Collection<?> collection = (Collection<?>) value;
                    for (Object obj : collection) {
                        if (obj != null) {
                            checkValueType(type, column, obj, strLen);
                            count++;
                        }
                    }
                    if (count > maxListCount) {
                        throw new RuntimeException(String.format("(%s) condition(%s), data Collection length(%s) has <= %s, current length(%s)",
                                name().toLowerCase(), column, count, maxListCount, count));
                    }
                } else {
                    throw new RuntimeException(String.format("%s condition(%s), data required has Collection",
                            name().toLowerCase(), column));
                }
            } else {
                checkValueType(type, column, value, strLen);
            }
        }
    }
    private void checkValueType(Class<?> type, String column, Object value, Integer strLen) {
        Object obj = toValue(type, value);
        if (QueryUtil.isNull(obj)) {
            throw new RuntimeException(String.format("column(%s) data(%s) has not %s type",
                    column, value, type.getSimpleName().toLowerCase()));
        }
        if (QueryUtil.isNotNull(strLen) && strLen > 0 && obj.toString().length() > strLen) {
            throw new RuntimeException(String.format("column(%s) data(%s) length can only be <= %s", column, value, strLen));
        }
    }

    private Object toValue(Class<?> type, Object value) {
        if (BOOLEAN_TYPE_SET.contains(type)) {
            return QueryUtil.toBoolean(value);
        } else if (INT_TYPE_SET.contains(type)) {
            return QueryUtil.toInteger(value);
        } else if (LONG_TYPE_SET.contains(type)) {
            return QueryUtil.toLonger(value);
        } else if (Number.class.isAssignableFrom(type)) {
            return QueryUtil.toDecimal(value);
        } else if (Date.class.isAssignableFrom(type)) {
            return QueryUtil.toDate(value);
        } else {
            return value;
        }
    }
}
