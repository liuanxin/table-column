package com.github.liuanxin.query.util;

import com.github.liuanxin.query.constant.QueryConst;
import com.github.liuanxin.query.enums.AliasGenerateRule;
import com.github.liuanxin.query.model.Table;
import com.github.liuanxin.query.model.TableColumn;
import com.github.liuanxin.query.model.TableColumnInfo;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class QueryUtil {

    private static final Map<String, Map<String, Field>> FIELDS_CACHE = new ConcurrentHashMap<>();

    private static final AtomicInteger TABLE_ALIAS = new AtomicInteger();
    private static final Map<String, Integer> TABLE_ALIAS_INT_MAP = new ConcurrentHashMap<>();
    private static final Map<String, String> TABLE_ALIAS_MAP = new ConcurrentHashMap<>();

    private static final Map<String, AtomicInteger> COLUMN_ALIAS = new ConcurrentHashMap<>();
    private static final Map<String, Integer> COLUMN_ALIAS_INT_MAP = new ConcurrentHashMap<>();
    private static final Map<String, String> COLUMN_ALIAS_MAP = new ConcurrentHashMap<>();


    private static final Map<String, DateTimeFormatter> FORMATTER_CACHE_MAP = new ConcurrentHashMap<>();

    private static DateTimeFormatter getFormatter(String type) {
        return FORMATTER_CACHE_MAP.computeIfAbsent(type, DateTimeFormatter::ofPattern);
    }
    private static DateTimeFormatter getFormatter(String type, String timezone) {
        return FORMATTER_CACHE_MAP.computeIfAbsent(type, s -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(s);
            TimeZone timeZone = TimeZone.getTimeZone(timezone);
            if (isNotNull(timeZone)) {
                formatter.withZone(timeZone.toZoneId());
            }
            return formatter;
        });
    }


    /** UserInfo --> user_info */
    public static String classToTableName(String tablePrefix, String className, String tableSuffix) {
        StringBuilder sbd = new StringBuilder();
        char[] chars = className.toCharArray();
        int len = chars.length;
        for (int i = 0; i < len; i++) {
            char c = chars[i];
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sbd.append("_");
                }
                sbd.append(Character.toLowerCase(c));
            } else {
                sbd.append(c);
            }
        }
        return toStr(tablePrefix) + sbd + toStr(tableSuffix);
    }

    /** userName --> user_name */
    public static String fieldToColumnName(String fieldName) {
        StringBuilder sbd = new StringBuilder();
        for (char c : fieldName.toCharArray()) {
            if (Character.isUpperCase(c)) {
                sbd.append("_").append(Character.toLowerCase(c));
            } else {
                sbd.append(c);
            }
        }
        return sbd.toString();
    }

    /** user_info | USER_INFO --> UserInfo */
    public static String tableNameToClass(String tablePrefix, String tableName) {
        return tableNameToClassAlias(tablePrefix, tableName, null);
    }

    public static String tableNameToClassAlias(String tablePrefix, String tableName, AliasGenerateRule aliasRule) {
        if (isEmpty(tableName)) {
            return "";
        }

        AliasGenerateRule rule = defaultIfNull(aliasRule, AliasGenerateRule.Standard);
        if (rule == AliasGenerateRule.Letter) {
            String tableAlias = TABLE_ALIAS_MAP.get(tableName);
            if (isNotEmpty(tableAlias)) {
                return tableAlias;
            } else {
                String ta = numTo26Radix(TABLE_ALIAS.incrementAndGet());
                TABLE_ALIAS_MAP.put(tableName, ta);
                return ta;
            }
        } else if (rule == AliasGenerateRule.Number) {
            Integer tableAlias = TABLE_ALIAS_INT_MAP.get(tableName);
            if (isNotNull(tableAlias)) {
                return tableAlias.toString();
            } else {
                int ta = 100000 + TABLE_ALIAS.incrementAndGet();
                TABLE_ALIAS_INT_MAP.put(tableName, ta);
                return String.valueOf(ta);
            }
        }

        String tn;
        if (isNotEmpty(tablePrefix) && tableName.toLowerCase().startsWith(tablePrefix.toLowerCase())) {
            tn = tableName.substring(tablePrefix.length());
        } else {
            tn = tableName;
        }
        switch (rule) {
            case Same: {
                return tn;
            }
            case Horizontal: {
                StringBuilder sbd = new StringBuilder();
                char[] chars = tn.toCharArray();
                int len = chars.length;
                sbd.append(Character.toUpperCase(chars[0]));
                for (int i = 1; i < len; i++) {
                    char c = chars[i];
                    if (c == '_') {
                        i++;
                        sbd.append("-").append(Character.toUpperCase(chars[i]));
                    } else {
                        sbd.append(Character.toLowerCase(c));
                    }
                }
                return sbd.toString();
            }
            case Under: {
                StringBuilder sbd = new StringBuilder();
                char[] chars = tn.toCharArray();
                int len = chars.length;
                sbd.append(Character.toUpperCase(chars[0]));
                for (int i = 1; i < len; i++) {
                    char c = chars[i];
                    if (c == '_') {
                        i++;
                        sbd.append("_").append(Character.toUpperCase(chars[i]));
                    } else {
                        sbd.append(Character.toLowerCase(c));
                    }
                }
                return sbd.toString();
            }
            case Lower: {
                return tn.toLowerCase();
            }
            case Upper: {
                return tn.toUpperCase();
            }
            default: {
                StringBuilder sbd = new StringBuilder();
                char[] chars = tn.toCharArray();
                int len = chars.length;
                sbd.append(Character.toUpperCase(chars[0]));
                for (int i = 1; i < len; i++) {
                    char c = chars[i];
                    if (c == '_') {
                        i++;
                        sbd.append(Character.toUpperCase(chars[i]));
                    } else {
                        sbd.append(Character.toLowerCase(c));
                    }
                }
                return sbd.toString();
            }
        }
    }

    /** user_name | USER_NAME --> userName */
    public static String columnNameToField(String columnName) {
        return columnNameToFieldAlias(columnName, "", null);
    }

    public static String columnNameToFieldAlias(String columnName, String tableName, AliasGenerateRule aliasRule) {
        if (isEmpty(columnName)) {
            return "";
        }

        AliasGenerateRule rule = defaultIfNull(aliasRule, AliasGenerateRule.Standard);
        switch (rule) {
            case Same: {
                return columnName;
            }
            case Horizontal: {
                StringBuilder sbd = new StringBuilder();
                char[] chars = columnName.toCharArray();
                int len = chars.length;
                for (int i = 0; i < len; i++) {
                    char c = chars[i];
                    if (c == '_') {
                        i++;
                        sbd.append("-").append(Character.toLowerCase(chars[i]));
                    } else {
                        sbd.append(Character.toLowerCase(c));
                    }
                }
                return sbd.toString();
            }
            case Under: {
                StringBuilder sbd = new StringBuilder();
                char[] chars = columnName.toCharArray();
                int len = chars.length;
                for (int i = 0; i < len; i++) {
                    char c = chars[i];
                    if (c == '_') {
                        i++;
                        sbd.append("_").append(Character.toLowerCase(chars[i]));
                    } else {
                        sbd.append(Character.toLowerCase(c));
                    }
                }
                return sbd.toString();
            }
            case Lower: {
                return columnName.toLowerCase();
            }
            case Upper: {
                return columnName.toUpperCase();
            }
            case Letter: {
                String key = toStr(tableName) + "-_-" + columnName;
                String columnAlias = COLUMN_ALIAS_MAP.get(key);
                if (isNotEmpty(columnAlias)) {
                    return columnAlias;
                } else {
                    int increment = COLUMN_ALIAS.computeIfAbsent(tableName, k -> new AtomicInteger()).incrementAndGet();
                    String ca = numTo26Radix(increment).toLowerCase();
                    COLUMN_ALIAS_MAP.put(key, ca);
                    return ca;
                }
            }
            case Number: {
                String key = toStr(tableName) + "-_-" + columnName;
                Integer columnAlias = COLUMN_ALIAS_INT_MAP.get(key);
                if (isNotNull(columnAlias)) {
                    return columnAlias.toString();
                } else {
                    int increment = COLUMN_ALIAS.computeIfAbsent(tableName, k -> new AtomicInteger()).incrementAndGet();
                    COLUMN_ALIAS_INT_MAP.put(key, increment);
                    return String.valueOf(increment);
                }
            }
            default: {
                StringBuilder sbd = new StringBuilder();
                char[] chars = columnName.toCharArray();
                int len = chars.length;
                for (int i = 0; i < len; i++) {
                    char c = chars[i];
                    if (c == '_') {
                        i++;
                        sbd.append(Character.toUpperCase(chars[i]));
                    } else {
                        sbd.append(Character.toLowerCase(c));
                    }
                }
                return sbd.toString();
            }
        }
    }

    /** 1 -> A, 26 -> Z, 27 -> AA, 702 : ZZ, 703 -> AAA ... */
    public static String numTo26Radix(int n) {
        StringBuilder s = new StringBuilder();
        while (n > 0) {
            int m = n % 26;
            if (m == 0) {
                m = 26;
            }
            s.insert(0, (char) (m + 64));
            n = (n - m) / 26;
        }
        return s.toString();
    }


    public static List<Field> getFields(Class<?> clazz) {
        return new ArrayList<>(getFields(clazz, 0).values());
    }
    private static Map<String, Field> getFields(Class<?> clazz, int depth) {
        if (clazz == null) {
            return Collections.emptyMap();
        }
        if (clazz == Object.class) {
            return Collections.emptyMap();
        }

        String key = clazz.getName();
        Map<String, Field> fieldCacheMap = FIELDS_CACHE.get(key);
        if (fieldCacheMap != null) {
            return fieldCacheMap;
        }

        Map<String, Field> returnMap = new LinkedHashMap<>();
        for (Field declaredField : clazz.getDeclaredFields()) {
            returnMap.put(declaredField.getName(), declaredField);
        }
        for (Field field : clazz.getFields()) {
            returnMap.put(field.getName(), field);
        }

        Class<?> superclass = clazz.getSuperclass();
        if (superclass != Object.class && depth <= 10) {
            Map<String, Field> fieldMap = getFields(superclass, depth + 1);
            if (isNotEmpty(fieldMap)) {
                returnMap.putAll(fieldMap);
            }
        }
        FIELDS_CACHE.put(key, returnMap);
        return returnMap;
    }
    public static Field getField(Class<?> clazz, String fieldName) {
        return getFields(clazz, 0).get(fieldName);
    }
    public static Object getFieldData(Class<?> clazz, String fieldName, Object obj) throws IllegalAccessException {
        Field field = getField(clazz, fieldName);
        if (isNotNull(field)) {
            boolean accessible = field.isAccessible();
            if (!accessible) {
                field.setAccessible(true);
            }
            Object fieldData = field.get(obj);
            if (!accessible) {
                field.setAccessible(false);
            }
            return fieldData;
        }
        return null;
    }

    public static Class<?> mappingClass(String dbType) {
        String type = dbType.toLowerCase();
        for (Map.Entry<String, Class<?>> entry : QueryConst.DB_TYPE_MAP.entrySet()) {
            if (type.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        throw new RuntimeException("unknown db type" + dbType);
    }

    public static boolean serializableToStr(Class<?> fieldType) {
        return QueryConst.SERIALIZE_STR_SET.contains(fieldType);
    }

    public static String toStr(Object obj) {
        return obj == null ? "" : obj.toString().trim();
    }

    public static String toString(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    public static String toStr(Collection<?> list) {
        if (isEmpty(list)) {
            return "";
        }
        StringJoiner sj = new StringJoiner(", ", "[", "]");
        for (Object obj : list) {
            sj.add(toStr(obj));
        }
        return sj.toString();
    }

    public static <T> String toStr(Collection<T> list, Function<T, String> func) {
        if (isEmpty(list)) {
            return "";
        }
        StringJoiner sj = new StringJoiner(",");
        for (T obj : list) {
            sj.add(func.apply(obj));
        }
        return sj.toString();
    }
    public static String toStr(Map<?, ?> map) {
        if (isEmpty(map)) {
            return "";
        }
        StringJoiner sj = new StringJoiner(", ", "{", "}");
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object v = entry.getValue();
            String value = Collection.class.isAssignableFrom(v.getClass()) ? toStr((Collection<?>) v) : toStr(v);
            sj.add(toStr(entry.getKey()) + " : " + value);
        }
        return sj.toString();
    }

    public static boolean toBool(Object obj) {
        return isNotNull(obj) && QueryConst.TRUE_SET.contains(obj.toString().toLowerCase());
    }

    public static Boolean toBoolean(Object obj) {
        if (isNotNull(obj)) {
            String str = obj.toString().trim().toLowerCase();
            if (QueryConst.BOOLEAN_SET.contains(str)) {
                return QueryConst.TRUE_SET.contains(str);
            }
        }
        return null;
    }

    public static int toInt(Object obj) {
        if (obj == null) {
            return 0;
        }
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static Integer toInteger(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static long toLong(Object obj) {
        if (obj == null) {
            return 0;
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        try {
            return Long.parseLong(obj.toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static Long toLonger(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        try {
            return Long.parseLong(obj.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static BigDecimal toDecimal(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof BigDecimal) {
            return (BigDecimal) obj;
        }
        try {
            return new BigDecimal(obj.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    public static Date toDate(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Date) {
            return (Date) obj;
        }
        String source = obj.toString().trim();
        for (String pattern : QueryConst.ALL_DATE_PATTERN_LIST) {
            try {
                Date date = new SimpleDateFormat(pattern).parse(source);
                if (date != null) {
                    return date;
                }
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    public static TemporalAccessor toLocalDate(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof TemporalAccessor) {
            return (TemporalAccessor) obj;
        }

        String source = obj.toString().trim();
        for (String pattern : QueryConst.DATE_TIME_PATTERN_LIST) {
            try {
                return getFormatter(pattern).parse(source, LocalDateTime::from);
            } catch (Exception ignore) {
            }
        }

        for (String pattern : QueryConst.DATE_PATTERN_LIST) {
            try {
                return getFormatter(pattern).parse(source, LocalDate::from);
            } catch (Exception ignore) {
            }
        }

        for (String pattern : QueryConst.TIME_PATTERN_LIST) {
            try {
                return getFormatter(pattern).parse(source, LocalTime::from);
            } catch (Exception ignore) {
            }
        }

        for (String pattern : QueryConst.YEAR_PATTERN_LIST) {
            try {
                return getFormatter(pattern).parse(source, Year::from);
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    public static String format(Date date) {
        return isNull(date) ? "" : format(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
    }

    public static String format(TemporalAccessor date, String type, String timezone) {
        return (isNull(date) || isEmpty(type)) ? "" : getFormatter(type, timezone).format(date);
    }
    public static String format(TemporalAccessor date) {
        if (isNull(date)) {
            return "";
        }

        String type;
        if (date instanceof LocalDateTime) {
            type = QueryConst.DEFAULT_DATE_TIME_FORMAT;
        } else if (date instanceof LocalDate) {
            type = QueryConst.DEFAULT_DATE_FORMAT;
        } else if (date instanceof LocalTime) {
            type = QueryConst.DEFAULT_TIME_FORMAT;
        } else if (date instanceof Year) {
            type = QueryConst.DEFAULT_YEAR_FORMAT;
        } else {
            throw new RuntimeException("unknown date type: " + date.getClass().getSimpleName());
        }
        return format(date, type, null);
    }

    public static boolean isNull(Object obj) {
        return obj == null;
    }
    public static boolean isNotNull(Object obj) {
        return obj != null;
    }

    public static boolean isIllegalId(Serializable id) {
        if (id == null) {
            return true;
        }
        if (id instanceof Number) {
            return ((Number) id).longValue() <= 0;
        }
        return isEmpty(id.toString());
    }
    public static boolean isIllegalIdList(List<Serializable> ids) {
        if (isEmpty(ids)) {
            return true;
        }
        for (Serializable id : ids) {
            if (!isIllegalId(id)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isEmpty(String str) {
        if (str == null) {
            return true;
        }
        String trim = str.trim().toLowerCase();
        return trim.isEmpty() || "null".equals(trim) /* || "nil".equals(trim) */ || "undefined".equals(trim);
    }
    public static boolean isNotEmpty(String obj) {
        return !isEmpty(obj);
    }

    public static boolean isEmpty(Collection<?> collection) {
        if (collection == null || collection.isEmpty()) {
            return true;
        }
        for (Object o : collection) {
            if (o != null) {
                return false;
            }
        }
        return true;
    }
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    public static boolean greater0(Number number) {
        return isNotNull(number) && number.intValue() > 0;
    }

    public static boolean isLong(Object obj) {
        if (obj == null) {
            return false;
        }

        try {
            Long.parseLong(obj.toString().trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    public static boolean isNotLong(Object obj) {
        return !isLong(obj);
    }

    public static boolean isDouble(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Number) {
            return true;
        }
        try {
            Double.parseDouble(obj.toString().trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    public static boolean isNotDouble(Object obj) {
        return !isDouble(obj);
    }

    public static <T> T defaultIfNull(T obj, T defaultObj) {
        return isNull(obj) ? defaultObj : obj;
    }
    public static String defaultIfBlank(String str, String defaultStr) {
        return isEmpty(str) ? defaultStr : str;
    }

    public static <T> T first(Collection<T> list) {
        return isEmpty(list) ? null : list.iterator().next();
    }

    public static <T> List<List<T>> split(List<T> list, int singleSize) {
        List<List<T>> returnList = new ArrayList<>();
        if (isNotEmpty(list) && singleSize > 0) {
            int size = list.size();
            int outLoop = (size % singleSize != 0) ? ((size / singleSize) + 1) : (size / singleSize);
            for (int i = 0; i < outLoop; i++) {
                List<T> innerList = new ArrayList<>();
                int j = (i * singleSize);
                int innerLoop = Math.min(j + singleSize, size);
                for (; j < innerLoop; j++) {
                    innerList.add(list.get(j));
                }
                returnList.add(innerList);
            }
        }
        return returnList;
    }


    public static String getTableName(String column, String mainTable) {
        return column.contains(".") ? column.split("\\.")[0].trim() : mainTable;
    }

    public static String getColumnName(String column) {
        return column.contains(".") ? column.split("\\.")[1].trim() : column.trim();
    }

    public static String getQueryColumn(boolean needAlias, String column, String mainTable, TableColumnInfo tcInfo) {
        Table table = tcInfo.findTable(getTableName(column, mainTable));
        TableColumn tableColumn = tcInfo.findTableColumn(table, getColumnName(column));
        String useColumnName = QuerySqlUtil.toSqlField(tableColumn.getName());
        if (needAlias) {
            return QuerySqlUtil.toSqlField(table.getAlias()) + "." + useColumnName;
        } else {
            return useColumnName;
        }
    }

    public static String getQueryColumnAndAlias(boolean needAlias, String column, String mainTable, TableColumnInfo tcInfo) {
        Table table = tcInfo.findTable(getTableName(column, mainTable));
        TableColumn tableColumn = tcInfo.findTableColumn(table, getColumnName(column));
        String tableColumnName = tableColumn.getName();
        String tableColumnAlias = tableColumn.getAlias();
        String useColumnName = QuerySqlUtil.toSqlField(tableColumnName);
        String alias = needAlias ? (QuerySqlUtil.toSqlField(table.getAlias()) + ".") : "";
        return alias + useColumnName + (tableColumnName.equals(tableColumnAlias) ? "" : (" AS " + QuerySqlUtil.toSqlField(tableColumnAlias)));
    }

    public static String getColumnGroup(boolean needAlias, String column, String mainTable, TableColumnInfo tcInfo) {
        Table table = tcInfo.findTable(getTableName(column, mainTable));
        TableColumn tableColumn = tcInfo.findTableColumn(table, getColumnName(column));
        String tableColumnName = tableColumn.getName();
        String tableColumnAlias = tableColumn.getAlias();
        if (needAlias) {
            return table.getAlias() + "_" + tableColumnAlias;
        } else if (tableColumnName.equals(tableColumnAlias)) {
            return QuerySqlUtil.toSqlField(tableColumnName);
        } else {
            return QuerySqlUtil.toSqlField(tableColumnAlias);
        }
    }

    public static String getColumnOrder(boolean needAlias, String column, String mainTable, TableColumnInfo tcInfo) {
        Table table = tcInfo.findTable(getTableName(column, mainTable));
        String tableColumnName = tcInfo.findTableColumn(table, getColumnName(column)).getName();
        if (needAlias) {
            return QuerySqlUtil.toSqlField(table.getAlias()) + "." + QuerySqlUtil.toSqlField(tableColumnName);
        } else {
            return QuerySqlUtil.toSqlField(tableColumnName);
        }
    }
}
