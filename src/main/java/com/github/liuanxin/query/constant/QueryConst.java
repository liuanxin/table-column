package com.github.liuanxin.query.constant;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.util.*;

public final class QueryConst {

    public static final String DB_SQL = "SELECT DATABASE()";
    public static final String TABLE_SQL = "SELECT `TABLE_NAME` tn, `TABLE_COMMENT` tc" +
            " FROM `information_schema`.`TABLES`" +
            " WHERE `TABLE_SCHEMA` = ?";
    public static final String COLUMN_SQL = "SELECT `TABLE_NAME` tn, `COLUMN_NAME` cn, `COLUMN_TYPE` ct," +
            " `COLUMN_COMMENT` cc, `COLUMN_KEY` ck, `CHARACTER_MAXIMUM_LENGTH` cml," +
            " `IS_NULLABLE` ine, `EXTRA` ext, `COLUMN_DEFAULT` cd" +
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

        DB_TYPE_MAP.put("timestamp", LocalDateTime.class);
        DB_TYPE_MAP.put("datetime", LocalDateTime.class);
        DB_TYPE_MAP.put("date", LocalDate.class);
        DB_TYPE_MAP.put("time", LocalTime.class);
        DB_TYPE_MAP.put("year", Year.class);

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

    public static final String DEFAULT_YEAR_FORMAT = "yyyy";
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";
    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final List<String> DATE_PATTERN_LIST = Arrays.asList(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd HH:mm",
            "yyyy/MM/dd",
            DEFAULT_DATE_TIME_FORMAT,
            "yyyy-MM-dd HH:mm",
            DEFAULT_DATE_FORMAT,
            DEFAULT_TIME_FORMAT,
            "HH:mm",
            DEFAULT_YEAR_FORMAT
    );

    public static final String TEMPLATE_META_NAME = "_meta_name_";
}
