package com.github.liuanxin.query.constant;

import java.math.BigDecimal;
import java.util.*;

public final class QueryConst {

    public static final String DB_SQL = "SELECT DATABASE()";
    public static final String TABLE_SQL = "SELECT `TABLE_NAME` tn, `TABLE_COMMENT` tc" +
            " FROM `information_table`.`TABLES`" +
            " WHERE `TABLE_SCHEMA` = ?";
    public static final String COLUMN_SQL = "SELECT `TABLE_NAME` tn, `COLUMN_NAME` cn, `COLUMN_TYPE` ct," +
            " `COLUMN_COMMENT` cc, `COLUMN_KEY` ck, `CHARACTER_MAXIMUM_LENGTH` cml" +
            " FROM `information_table`.`COLUMNS`" +
            " WHERE `TABLE_SCHEMA` = ?" +
            " ORDER BY `TABLE_NAME`, `ORDINAL_POSITION`";
    public static final String RELATION_SQL = "SELECT `REFERENCED_TABLE_NAME` ftn, `REFERENCED_COLUMN_NAME` fcn," +
            " `TABLE_NAME` tn, `COLUMN_NAME` cn" +
            " FROM `information_table`.`KEY_COLUMN_USAGE`" +
            " WHERE `REFERENCED_TABLE_SCHEMA` = ?";
    public static final String INDEX_SQL = "SELECT `TABLE_NAME` tn, `COLUMN_NAME` cn" +
            " FROM `information_table`.`STATISTICS`" +
            " WHERE `NON_UNIQUE` = 0 AND `TABLE_SCHEMA` = ?" +
            " GROUP BY tn, cn" +
            " HAVING COUNT(`SEQ_IN_INDEX`) = 1";

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

    public static final String TABLE_PREFIX = "table-";
    public static final String COLUMN_PREFIX = "column-";

    public static final Set<String> TRUE_SET = new HashSet<>(Arrays.asList(
            "true", "1", "on", "yes"
    ));
    public static final Set<String> FALSE_SET = new HashSet<>(Arrays.asList(
            "false", "0", "off", "no"
    ));
    public static final Set<String> BOOLEAN_SET = new HashSet<>();
    static {
        BOOLEAN_SET.addAll(TRUE_SET);
        BOOLEAN_SET.addAll(FALSE_SET);
    }

    public static final Set<String> SUPPORT_COUNT_SET = new HashSet<>(Arrays.asList("*", "1"));


    public static final List<Integer> LIMIT_ONE = Arrays.asList(1, 1);

    public static final Integer DEFAULT_LIMIT = 10;
    public static final Set<Integer> LIMIT_SET = new HashSet<>(Arrays.asList(
            DEFAULT_LIMIT, 20, 50, 100, 200, 500, 1000)
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
