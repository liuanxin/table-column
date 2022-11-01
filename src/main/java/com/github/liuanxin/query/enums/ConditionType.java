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
 *   is null
 *   is not null
 *   =
 *   <>
 *   in     (list)
 *   not in (list)
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
 *   not like
 * </pre>
 */
public enum ConditionType {

    /*
    nu  : IS NULL     为空
    nn  : IS NOT NULL 不为空

    eq  : =           等于
    ne  : <>          不等于

    in  : in          包含
    ni  : NOT IN      不包含

    bet : BETWEEN     区间
    nbe : NOT BETWEEN 不在区间

    gt  : >           大于
    ge  : >=          大于等于
    lt  : <           小于
    le  : <=          小于等于

    fuzzy : LIKE '%x%'  模糊
    start : LIKE 'x%'   开头
    end   : LIKE '%x'   结尾
    */

    NU("IS NULL", "为空") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateCondition(column, printSql);
        }
    },
    NN("IS NOT NULL", "不为空") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateCondition(column, printSql);
        }
    },

    EQ("=", "等于") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateCondition(column, type, value, params, printSql);
        }
    },
    NE("<>", "不等于") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateCondition(column, type, value, params, printSql);
        }
    },

    IN("IN", "包含") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateMulti(column, type, value, params, printSql);
        }
    },
    NI("NOT IN", "不包含") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateMulti(column, type, value, params, printSql);
        }
    },

    BET("BETWEEN", "区间") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateMulti(column, type, value, params, printSql);
        }
    },
    NBE("NOT BETWEEN", "不在区间") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateMulti(column, type, value, params, printSql);
        }
    },
    GT(">", "大于") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateCondition(column, type, value, params, printSql);
        }
    },
    GE(">=", "大于等于") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateCondition(column, type, value, params, printSql);
        }
    },
    LT("<", "小于") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateCondition(column, type, value, params, printSql);
        }
    },
    LE("<=", "小于等于") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateCondition(column, type, value, params, printSql);
        }
    },

    FUZZY("LIKE", "模糊") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateCondition(column, type, ("%" + value + "%"), params, printSql);
        }
    },
    START("LIKE", "开头") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateCondition(column, type, (value + "%"), params, printSql);
        }
    },
    END("LIKE", "结尾") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateCondition(column, type, ("%" + value), params, printSql);
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


    public abstract String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql);


    public String info() {
        return name().toLowerCase() + "(" + msg + ")";
    }

    public void checkTypeAndValue(Class<?> type, String column, Object value, Integer strLen, int maxListCount) {
        checkType(type, column);
        checkValue(type, column, value, strLen, maxListCount);
    }


    protected String generateCondition(String column, StringBuilder printSql) {
        String sqlField = QuerySqlUtil.toSqlField(column);
        String value = getValue();
        printSql.append(sqlField).append(value);
        return String.format("%s %s", sqlField, value);
    }
    protected String generateCondition(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
        if (value == null) {
            return "";
        } else {
            params.add(QuerySqlUtil.toValue(type, value));
            printSql.append(String.format("%s %s %s", column, getValue(), QuerySqlUtil.toPrintValue(type, value)));
            return String.format("%s %s ?", column, getValue());
        }
    }
    protected String generateMulti(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
        if (value == null || !MULTI_TYPE.contains(this) || !(value instanceof Collection<?>)) {
            return "";
        }
        Collection<?> c = (Collection<?>) value;
        if (QueryUtil.isEmpty(c)) {
            return "";
        }

        if (this == BET || this == NBE) {
            Object[] arr = c.toArray();
            Object start = arr[0];
            Object end = arr.length > 1 ? arr[1] : null;

            StringBuilder sbd = new StringBuilder();
            if (QueryUtil.isNotNull(start) && QueryUtil.isNotNull(end)) {
                params.add(QuerySqlUtil.toValue(type, start));
                params.add(QuerySqlUtil.toValue(type, end));
                printSql.append(column).append(" BETWEEN ").append(QuerySqlUtil.toPrintValue(type, start))
                        .append(" AND ").append(QuerySqlUtil.toPrintValue(type, end));
                sbd.append(column).append(" BETWEEN ? AND ?");
            } else {
                if (QueryUtil.isNotNull(start)) {
                    params.add(QuerySqlUtil.toValue(type, start));
                    printSql.append(column).append(" >= ").append(QuerySqlUtil.toPrintValue(type, start));
                    sbd.append(column).append(" >= ?");
                }
                if (QueryUtil.isNotNull(end)) {
                    params.add(QuerySqlUtil.toValue(type, end));
                    printSql.append(column).append(" <= ").append(QuerySqlUtil.toPrintValue(type, end));
                    sbd.append(column).append(" <= ?");
                }
            }
            return sbd.toString();
        } else {
            boolean hasChange = false;
            StringJoiner sj = new StringJoiner(", ");
            StringJoiner printSj = new StringJoiner(", ");
            for (Object obj : c) {
                if (obj != null) {
                    if (!hasChange) {
                        hasChange = true;
                    }
                    params.add(QuerySqlUtil.toValue(type, obj));
                    printSj.add(QuerySqlUtil.toPrintValue(type, obj));
                    sj.add("?");
                }
            }
            if (hasChange) {
                printSql.append(String.format("%s %s (%s)", column, getValue(), printSj));
                return String.format("%s %s (%s)", column, getValue(), sj);
            } else {
                return "";
            }
        }
    }


    private static final Set<ConditionType> MULTI_TYPE = new HashSet<>(Arrays.asList(
            IN,
            NI,
            BET,
            NBE
    ));
    /** string: 等于(eq)、不等于(ne)、包含(in)、不包含(ni)、包含(include)、开头(start)、结尾(end) */
    private static final Set<ConditionType> STRING_TYPE_SET = new LinkedHashSet<>(Arrays.asList(
            EQ,
            NE,
            IN,
            NI,
            FUZZY,
            START,
            END
    ));
    private static final String STRING_TYPE_INFO = String.format("String type can only be used in 「%s」 conditions",
            STRING_TYPE_SET.stream().map(ConditionType::info).collect(Collectors.joining(", ")));

    /** number: 等于(eq)、不等于(ne)、包含(in)、不包含(ni)、大于(gt)、大于等于(ge)、小于(lt)、小于等于(le)、区间(bet)、不在区间(nbe) */
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

    /**  非 string/number/date 类型: 等于(eq)、不等于(ne)、包含(in)、不包含(ni)、为空(nu)、不为空(nn) */
    public static final Set<ConditionType> OTHER_TYPE_SET = new HashSet<>(Arrays.asList(
            EQ,
            NE,
            IN,
            NI,
            NU,
            NN
    ));
    private static final String OTHER_TYPE_INFO = String.format("Non(String, Number, Date) type can only be used in 「%s」 conditions",
            OTHER_TYPE_SET.stream().map(ConditionType::info).collect(Collectors.joining(", ")));


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
                        throw new RuntimeException(String.format("(%s) condition, column(%s), data Collection length has be <= %s, current length(%s)",
                                info(), column, maxListCount, count));
                    }
                } else {
                    throw new RuntimeException(String.format("%s condition, column(%s), data required has Collection",
                            info(), column));
                }
            } else {
                checkValueType(type, column, value, strLen);
            }
        }
    }
    private void checkValueType(Class<?> type, String column, Object value, Integer strLen) {
        Object obj = QuerySqlUtil.toValue(type, value);
        if (QueryUtil.isNull(obj)) {
            throw new RuntimeException(String.format("column(%s) data(%s) has not %s type",
                    column, value, type.getSimpleName().toLowerCase()));
        }
        if (QueryUtil.isNotNull(strLen) && strLen > 0 && obj.toString().length() > strLen) {
            throw new RuntimeException(String.format("column(%s) data(%s) length can only be <= %s", column, value, strLen));
        }
    }
}
