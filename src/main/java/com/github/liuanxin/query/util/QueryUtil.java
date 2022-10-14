package com.github.liuanxin.query.util;

import com.github.liuanxin.query.constant.QueryConst;
import com.github.liuanxin.query.model.Table;
import com.github.liuanxin.query.model.TableColumn;
import com.github.liuanxin.query.model.TableColumnInfo;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QueryUtil {

    private static final Map<String, Map<String, Field>> FIELDS_CACHE = new ConcurrentHashMap<>();


    /** UserInfo --> user_info */
    public static String aliasToTableName(String tablePrefix, String className) {
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
        return toStr(tablePrefix) + sbd;
    }

    /** userName --> user_name */
    public static String aliasToColumnName(String fieldName) {
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
    public static String tableNameToAlias(String tableName) {
        if (tableName.toLowerCase().startsWith("t_")) {
            tableName = tableName.substring(2);
        }
        StringBuilder sbd = new StringBuilder();
        char[] chars = tableName.toCharArray();
        sbd.append(Character.toUpperCase(chars[0]));
        int len = chars.length;
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

    /** user_name | USER_NAME --> userName */
    public static String columnNameToAlias(String columnName) {
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
        Field[] declaredFields = clazz.getDeclaredFields();
        if (declaredFields.length > 0) {
            for (Field declaredField : declaredFields) {
                returnMap.put(declaredField.getName(), declaredField);
            }
        }
        Field[] fields = clazz.getFields();
        if (fields.length > 0) {
            for (Field field : fields) {
                returnMap.put(field.getName(), field);
            }
        }

        Class<?> superclass = clazz.getSuperclass();
        if (superclass != Object.class && depth <= 10) {
            Map<String, Field> fieldMap = getFields(superclass, depth + 1);
            if (!fieldMap.isEmpty()) {
                returnMap.putAll(fieldMap);
            }
        }
        FIELDS_CACHE.put(key, returnMap);
        return returnMap;
    }
    public static Field getField(Class<?> clazz, String fieldName) {
        return getFields(clazz, 0).get(fieldName);
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

    public static String toStr(Object obj) {
        return obj == null ? "" : obj.toString().trim();
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
        try {
            return new BigDecimal(obj.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    public static Date toDate(Object obj) {
        for (String pattern : QueryConst.DATE_PATTERN_LIST) {
            try {
                Date date = new SimpleDateFormat(pattern).parse(obj.toString().trim());
                if (date != null) {
                    return date;
                }
            } catch (ParseException ignore) {
            }
        }
        return null;
    }

    public static String formatDate(Date date, String pattern, String timezone) {
        if (date == null) {
            return null;
        }
        try {
            SimpleDateFormat df = new SimpleDateFormat(pattern);
            if (timezone != null && !timezone.trim().isEmpty()) {
                df.setTimeZone(TimeZone.getTimeZone(timezone.trim()));
            }
            return df.format(date);
        } catch (Exception e) {
            return formatDate(date);
        }
    }
    public static String formatDate(Date date) {
        return new SimpleDateFormat(QueryConst.DEFAULT_DATE_FORMAT).format(date);
    }

    public static boolean isBoolean(Object obj) {
        return obj != null && new HashSet<>(Arrays.asList(
                "true", "1", "on", "yes",
                "false", "0", "off", "no"
        )).contains(obj.toString().toLowerCase());
    }

    public static boolean isBlank(Object obj) {
        if (obj == null) {
            return true;
        }
        if (obj instanceof String) {
            if (((String) obj).isEmpty()) {
                return true;
            }
        }
        if (obj instanceof Collection<?>) {
            if (((Collection<?>) obj).isEmpty()) {
                return true;
            }
        }
        return (obj instanceof Map) && ((Map<?, ?>) obj).isEmpty();
    }
    public static boolean isNotBlank(Object obj) {
        return !isBlank(obj);
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

    public static String defaultIfBlank(String str1, String defaultStr) {
        return (str1 == null || str1.isEmpty()) ? defaultStr : str1;
    }

    public static <T> T first(Collection<T> list) {
        return (list == null || list.isEmpty()) ? null : list.iterator().next();
    }

    public static <T> List<List<T>> split(List<T> list, int singleSize) {
        List<List<T>> returnList = new ArrayList<>();
        if (list != null && !list.isEmpty() && singleSize > 0) {
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

    public static String getUseColumn(boolean needAlias, String column, String mainTable, TableColumnInfo tcInfo) {
        String tableName = getTableName(column, mainTable);
        String columnName = getColumnName(column);
        Table table = tcInfo.findTable(tableName);
        TableColumn tableColumn = tcInfo.findTableColumn(table, columnName);
        String useColumnName = QuerySqlUtil.toSqlField(tableColumn.getName());
        if (needAlias) {
            String alias = table.getAlias();
            return QuerySqlUtil.toSqlField(alias) + "." + useColumnName + " AS " + alias + "_" + tableColumn.getName();
        } else {
            return useColumnName;
        }
    }

    public static String getUseQueryColumn(boolean needAlias, String column, String mainTable, TableColumnInfo tcInfo) {
        String tableName = getTableName(column, mainTable);
        String columnName = getColumnName(column);
        Table table = tcInfo.findTable(tableName);
        TableColumn tableColumn = tcInfo.findTableColumn(table, columnName);
        String tableColumnName = tableColumn.getName();
        String tableColumnAlias = tableColumn.getAlias();
        String useColumnName = QuerySqlUtil.toSqlField(tableColumnName);
        if (needAlias) {
            String alias = table.getAlias();
            return QuerySqlUtil.toSqlField(alias) + "." + useColumnName + " AS " + alias + "_" + tableColumnAlias;
        } else {
            return useColumnName + (tableColumnName.equals(tableColumnAlias) ? "" : (" AS " + tableColumnAlias));
        }
    }
}
