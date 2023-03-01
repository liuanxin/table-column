package com.github.liuanxin.query.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.liuanxin.query.util.QuerySqlUtil;
import com.github.liuanxin.query.util.QueryUtil;

import java.util.*;

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
    $nu     : IS NULL      为空
    $nn     : IS NOT NULL  不为空
    $eq     : =            等于
    $ne     : <>           不等于
    $in     : IN           包含
    $ni     : NOT IN       不包含
    $bet    : BETWEEN      区间
    $nbe    : NOT BETWEEN  不在区间
    $gt     : >            大于
    $ge     : >=           大于等于
    $lt     : <            小于
    $le     : <=           小于等于

    $fuzzy  : LIKE '%x%'     模糊
    $nfuzzy : NOT LIKE '%x%' 不模糊
    $start  : LIKE 'x%'      开头
    $nstart : NOT LIKE 'x%'  不开头
    $end    : LIKE '%x'      结尾
    $nend   : NOT LIKE '%x'  不结尾
    */

    $NU("IS NULL", "为空") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateCondition(column, printSql);
        }
    },
    $NN("IS NOT NULL", "不为空") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateCondition(column, printSql);
        }
    },

    $EQ("=", "等于") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateCondition(column, type, value, params, printSql);
        }
    },
    $NE("<>", "不等于") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateCondition(column, type, value, params, printSql);
        }
    },

    $IN("IN", "包含") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateMulti(column, type, value, params, printSql);
        }
    },
    $NI("NOT IN", "不包含") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateMulti(column, type, value, params, printSql);
        }
    },

    $BET("BETWEEN", "区间") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateMulti(column, type, value, params, printSql);
        }
    },
    $NBE("NOT BETWEEN", "不在区间") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateMulti(column, type, value, params, printSql);
        }
    },
    $GT(">", "大于") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateCondition(column, type, value, params, printSql);
        }
    },
    $GE(">=", "大于等于") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateCondition(column, type, value, params, printSql);
        }
    },
    $LT("<", "小于") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateCondition(column, type, value, params, printSql);
        }
    },
    $LE("<=", "小于等于") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateCondition(column, type, value, params, printSql);
        }
    },

    $FUZZY("LIKE", "模糊") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateCondition(column, type, ("%" + value + "%"), params, printSql);
        }
    },
    $NFUZZY("NOT LIKE", "不模糊") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateCondition(column, type, ("%" + value + "%"), params, printSql);
        }
    },
    $START("LIKE", "开头") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateCondition(column, type, (value + "%"), params, printSql);
        }
    },
    $NSTART("NOT LIKE", "不开头") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateCondition(column, type, (value + "%"), params, printSql);
        }
    },
    $END("LIKE", "结尾") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateCondition(column, type, ("%" + value), params, printSql);
        }
    },
    $NEND("NOT LIKE", "不结尾") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
            return generateCondition(column, type, ("%" + value), params, printSql);
        }
    };


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

    @JsonValue
    public String value() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static ConditionType deserializer(Object obj) {
        if (obj != null) {
            String str = obj.toString().trim();
            for (ConditionType e : values()) {
                if (str.equalsIgnoreCase(e.name())) {
                    return e;
                }
                if (str.equalsIgnoreCase(e.value)) {
                    return e;
                }
                if (str.equals(e.msg)) {
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
        String sql = String.format("%s %s", sqlField, value);
        printSql.append(sql);
        return sql;
    }
    protected String generateCondition(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
        if (QueryUtil.isNull(value)) {
            return "";
        } else {
            params.add(QuerySqlUtil.toValue(type, value));
            printSql.append(String.format("%s %s %s", column, getValue(), QuerySqlUtil.toPrintValue(type, value)));
            return String.format("%s %s ?", column, getValue());
        }
    }
    protected String generateMulti(String column, Class<?> type, Object value, List<Object> params, StringBuilder printSql) {
        if (QueryUtil.isNull(value) || !MULTI$TYPE.contains(this) || !(value instanceof Collection<?>)) {
            return "";
        }
        Collection<?> c = (Collection<?>) value;
        if (QueryUtil.isEmpty(c)) {
            return "";
        }

        if (this == $BET || this == $NBE) {
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
                if (QueryUtil.isNotNull(obj)) {
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


    private static final Set<ConditionType> MULTI$TYPE = new HashSet<>(Arrays.asList(
            $IN,
            $NI,
            $BET,
            $NBE
    ));
    /** string: 为空(nu)、不为空(NN)、等于(eq)、不等于(ne)、包含(in)、不包含(ni)、包含(fuzzy)、不包含(nfuzzy)、开头(start)、不开头(nstart)、结尾(end)、不结尾(nend) */
    private static final Set<ConditionType> STRING$TYPE$SET = new LinkedHashSet<>(Arrays.asList(
            $NU,
            $NN,
            $EQ,
            $NE,
            $IN,
            $NI,
            $FUZZY,
            $NFUZZY,
            $START,
            $NSTART,
            $END,
            $NEND
    ));
    private static final String STRING$TYPE$INFO = String.format("String type can only be used in 「%s」 conditions",
            QueryUtil.toStr(STRING$TYPE$SET, ConditionType::info));

    /** number: 为空(nu)、不为空(NN)、等于(eq)、不等于(ne)、包含(in)、不包含(ni)、大于(gt)、大于等于(ge)、小于(lt)、小于等于(le)、区间(bet)、不在区间(nbe) */
    private static final Set<ConditionType> NUMBER$TYPE$SET = new LinkedHashSet<>(Arrays.asList(
            $NU,
            $NN,
            $EQ,
            $NE,
            $IN,
            $NI,
            $GT,
            $GE,
            $LT,
            $LE,
            $BET,
            $NBE
    ));
    private static final String NUMBER$TYPE$INFO = String.format("Number type can only be used in 「%s」 conditions",
            QueryUtil.toStr(NUMBER$TYPE$SET, ConditionType::info));

    /** date: 为空(nu)、不为空(NN)、大于(gt)、大于等于(ge)、小于(lt)、小于等于(le)、区间(bet)、不在区间(nbe) */
    private static final Set<ConditionType> DATE$TYPE$SET = new LinkedHashSet<>(Arrays.asList(
            $NU,
            $NN,
            $GT,
            $GE,
            $LT,
            $LE,
            $BET,
            $NBE
    ));
    private static final String DATE$TYPE$INFO = String.format("Date type can only be used in 「%s」 conditions",
            QueryUtil.toStr(DATE$TYPE$SET, ConditionType::info));

    /**  非 string/number/date 类型: 为空(nu)、不为空(NN)、等于(eq)、不等于(ne)、包含(in)、不包含(ni)、为空(nu)、不为空(nn) */
    public static final Set<ConditionType> OTHER$TYPE$SET = new HashSet<>(Arrays.asList(
            $NU,
            $NN,
            $EQ,
            $NE,
            $IN,
            $NI,
            $NU,
            $NN
    ));
    private static final String OTHER$TYPE$INFO = String.format("Non(String, Number, Date) type can only be used in 「%s」 conditions",
            QueryUtil.toStr(OTHER$TYPE$SET, ConditionType::info));


    private void checkType(Class<?> type, String column) {
        if (Number.class.isAssignableFrom(type)) {
            if (!NUMBER$TYPE$SET.contains(this)) {
                throw new RuntimeException(column + ": " + NUMBER$TYPE$INFO);
            }
        } else if (Date.class.isAssignableFrom(type)) {
            if (!DATE$TYPE$SET.contains(this)) {
                throw new RuntimeException(column + ": " + DATE$TYPE$INFO);
            }
        } else if (String.class.isAssignableFrom(type)) {
            if (!STRING$TYPE$SET.contains(this)) {
                throw new RuntimeException(column + ": " + STRING$TYPE$INFO);
            }
        } else {
            if (!OTHER$TYPE$SET.contains(this)) {
                throw new RuntimeException(column + ": " + OTHER$TYPE$INFO);
            }
        }
    }

    private void checkValue(Class<?> type, String column, Object value, Integer strLen, int maxListCount) {
        if (QueryUtil.isNotNull(value)) {
            if (MULTI$TYPE.contains(this)) {
                if (value instanceof Collection<?>) {
                    int count = 0;
                    Collection<?> collection = (Collection<?>) value;
                    for (Object obj : collection) {
                        if (QueryUtil.isNotNull(obj)) {
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
