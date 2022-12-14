package com.github.liuanxin.query.constant;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public final class QueryConst {

    public static final String DB_SQL = "SELECT DATABASE()";
    public static final String TABLE_SQL = "SELECT `TABLE_NAME` tn, `TABLE_COMMENT` tc" +
            " FROM `information_schema`.`TABLES`" +
            " WHERE `TABLE_SCHEMA` = ?";
    public static final String COLUMN_SQL = "SELECT `TABLE_NAME` tn, `COLUMN_NAME` cn, `COLUMN_TYPE` ct," +
            " `COLUMN_COMMENT` cc, `COLUMN_KEY` ck, `CHARACTER_MAXIMUM_LENGTH` cml," +
            " `IS_NULLABLE` ine, `EXTRA` ex, `COLUMN_DEFAULT` cd" +
            " FROM `information_schema`.`COLUMNS`" +
            " WHERE `TABLE_SCHEMA` = ?" +
            " ORDER BY `TABLE_NAME`, `ORDINAL_POSITION`";

    public static final Map<String, Class<?>> DB_TYPE_MAP = new LinkedHashMap<>();
    static {
        DB_TYPE_MAP.put("tinyint(1) unsigned", Integer.class);
        DB_TYPE_MAP.put("tinyint(1)", Boolean.class);
        DB_TYPE_MAP.put("bigint", Long.class);
        DB_TYPE_MAP.put("int", Integer.class);

        DB_TYPE_MAP.put("char", String.class);
        DB_TYPE_MAP.put("text", String.class);

        DB_TYPE_MAP.put("date", Date.class);
        DB_TYPE_MAP.put("time", Date.class);
        DB_TYPE_MAP.put("year", Date.class);

        DB_TYPE_MAP.put("decimal", BigDecimal.class);
        DB_TYPE_MAP.put("float", Float.class);
        DB_TYPE_MAP.put("double", Double.class);
    }
    public static final Set<Class<?>> SERIALIZE_STR_SET = new HashSet<>(Arrays.asList(
            Long.class, BigInteger.class, BigDecimal.class, Float.class, Double.class
    ));

    public static final String TABLE_PREFIX = "__table__";
    public static final String COLUMN_PREFIX = "__column__";

    public static final String COUNT_ALIAS = "cnt";

    public static final Set<String> TRUE_SET = new HashSet<>(Arrays.asList(
            "true", "1", "on", "yes" // , "✓"/* \u2713 */, "✔"/* \u2714 */
    ));
    public static final Set<String> FALSE_SET = new HashSet<>(Arrays.asList(
            "false", "0", "off", "no" //, "✗"/* \u2717 */, "✘"/* \u2718 */
    ));
    public static final Set<String> BOOLEAN_SET = new HashSet<>();
    static {
        BOOLEAN_SET.addAll(TRUE_SET);
        BOOLEAN_SET.addAll(FALSE_SET);
    }

    public static final Set<Class<?>> BOOLEAN_TYPE_SET = new HashSet<>(Arrays.asList(Boolean.class, boolean.class));
    public static final Set<Class<?>> INT_TYPE_SET = new HashSet<>(Arrays.asList(Integer.class, int.class));
    public static final Set<Class<?>> LONG_TYPE_SET = new HashSet<>(Arrays.asList(Long.class, long.class));

    public static final Set<String> SUPPORT_COUNT_SET = new HashSet<>(Arrays.asList("*", "1"));


    public static final Integer DEFAULT_LIMIT = 10;
    public static final List<Integer> LIMIT_ONE = Arrays.asList(1, 1);
    public static final Set<Integer> LIMIT_SET = new LinkedHashSet<>(Arrays.asList(
            1, 2, 3, 5, DEFAULT_LIMIT, 20, 30, 50, 100, 200, 300, 500, 1000)
    );

    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final List<String> DATE_PATTERN_LIST = Arrays.asList(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd HH:mm",
            "yyyy/MM/dd",
            DEFAULT_DATE_FORMAT,
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd",
            "HH:mm:ss",
            "HH:mm"
    );
}
