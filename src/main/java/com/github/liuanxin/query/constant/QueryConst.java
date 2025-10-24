package com.github.liuanxin.query.constant;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.util.*;

public final class QueryConst {

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
    public static final Map<Class<?>, String> TYPE_MAP = new HashMap<>();
    static {
        TYPE_MAP.put(Integer.class, "int");
        TYPE_MAP.put(Boolean.class, "boolean");
        TYPE_MAP.put(Long.class, "int");

        TYPE_MAP.put(String.class, "string");

        TYPE_MAP.put(LocalDateTime.class, "date-time");
        TYPE_MAP.put(LocalDate.class, "date");
        TYPE_MAP.put(LocalTime.class, "time");
        TYPE_MAP.put(Year.class, "year");

        TYPE_MAP.put(BigDecimal.class, "number");
        TYPE_MAP.put(Float.class, "number");
        TYPE_MAP.put(Double.class, "number");

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

    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final List<String> DATE_TIME_PATTERN_LIST = Arrays.asList(
            DEFAULT_DATE_TIME_FORMAT,
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd HH",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd HH:mm",
            "yyyy/MM/dd HH"
    );
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final List<String> DATE_PATTERN_LIST = Arrays.asList(
            DEFAULT_DATE_FORMAT,
            "yyyy-MM",
            "yyyy/MM/dd",
            "yyyy/MM"
    );
    public static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";
    public static final List<String> TIME_PATTERN_LIST = Arrays.asList(
            DEFAULT_TIME_FORMAT,
            "HH:mm"
    );
    public static final String DEFAULT_YEAR_FORMAT = "yyyy";
    public static final List<String> YEAR_PATTERN_LIST = Arrays.asList(
            DEFAULT_YEAR_FORMAT,
            "yy"
    );
    public static final List<String> ALL_DATE_PATTERN_LIST = new ArrayList<>();
    static {
        ALL_DATE_PATTERN_LIST.addAll(DATE_TIME_PATTERN_LIST);
        ALL_DATE_PATTERN_LIST.addAll(DATE_PATTERN_LIST);
        ALL_DATE_PATTERN_LIST.addAll(TIME_PATTERN_LIST);
        ALL_DATE_PATTERN_LIST.addAll(YEAR_PATTERN_LIST);
    }

    public static final String TEMPLATE_META_NAME = "_meta_name_";
}
