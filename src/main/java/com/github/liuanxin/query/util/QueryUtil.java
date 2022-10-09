package com.github.liuanxin.query.util;

import com.github.liuanxin.query.annotation.ColumnInfo;
import com.github.liuanxin.query.annotation.SchemaInfo;
import com.github.liuanxin.query.constant.QueryConst;
import com.github.liuanxin.query.enums.SchemaRelationType;
import com.github.liuanxin.query.model.Schema;
import com.github.liuanxin.query.model.SchemaColumn;
import com.github.liuanxin.query.model.SchemaColumnInfo;
import com.github.liuanxin.query.model.SchemaColumnRelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QueryUtil {

    private static final Logger LOG = LoggerFactory.getLogger(QueryUtil.class);

    private static final PathMatchingResourcePatternResolver RESOLVER =
            new PathMatchingResourcePatternResolver(ClassLoader.getSystemClassLoader());

    private static final MetadataReaderFactory READER = new CachingMetadataReaderFactory(RESOLVER);
    private static final Map<String, Map<String, Field>> FIELDS_CACHE = new ConcurrentHashMap<>();


    public static SchemaColumnInfo scanSchema(String classPackages) {
        return handleSchema(scanPackage(classPackages));
    }
    private static Set<Class<?>> scanPackage(String classPackages) {
        if (classPackages == null) {
            return Collections.emptySet();
        }

        Set<Class<?>> set = new LinkedHashSet<>();
        for (String cp : classPackages.split(",")) {
            String path = (cp != null) ? cp.trim() : null;
            if (path != null && !path.isEmpty()) {
                try {
                    String location = String.format("classpath*:**/%s/**/*.class", path.replace(".", "/"));
                    for (Resource resource : RESOLVER.getResources(location)) {
                        if (resource.isReadable()) {
                            String className = READER.getMetadataReader(resource).getClassMetadata().getClassName();
                            set.add(Class.forName(className));
                        }
                    }
                } catch (Exception e) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("get({}) class exception", path, e);
                    }
                }
            }
        }
        return set;
    }
    private static SchemaColumnInfo handleSchema(Set<Class<?>> classes) {
        Map<String, String> aliasMap = new HashMap<>();
        Map<String, String> schemaClassMap = new HashMap<>();
        Map<String, Schema> schemaMap = new LinkedHashMap<>();
        List<SchemaColumnRelation> relationList = new ArrayList<>();

        Map<String, ColumnInfo> columnInfoMap = new LinkedHashMap<>();
        Map<String, Class<?>> columnClassMap = new HashMap<>();
        Set<String> schemaNameSet = new HashSet<>();
        Set<String> schemaAliasSet = new HashSet<>();
        Set<String> columnNameSet = new HashSet<>();
        Set<String> columnAliasSet = new HashSet<>();
        for (Class<?> clazz : classes) {
            SchemaInfo schemaInfo = clazz.getAnnotation(SchemaInfo.class);
            String schemaName, schemaDesc, schemaAlias;
            if (schemaInfo != null) {
                if (schemaInfo.ignore()) {
                    continue;
                }

                schemaName = schemaInfo.value();
                schemaDesc = schemaInfo.desc();
                schemaAlias = defaultIfBlank(schemaInfo.alias(), schemaName);
            } else {
                schemaDesc = "";
                schemaAlias = clazz.getSimpleName();
                schemaName = aliasToSchemaName(schemaAlias);
            }

            if (schemaNameSet.contains(schemaName)) {
                throw new RuntimeException("schema(" + schemaName + ") has renamed");
            }
            schemaNameSet.add(schemaName);
            if (schemaAliasSet.contains(schemaAlias)) {
                throw new RuntimeException("schema alias(" + schemaName + ") has renamed");
            }
            schemaAliasSet.add(schemaAlias);

            Map<String, SchemaColumn> columnMap = new LinkedHashMap<>();
            for (Field field : getFields(clazz)) {
                ColumnInfo columnInfo = field.getAnnotation(ColumnInfo.class);
                Class<?> fieldType = field.getType();
                String columnName, columnDesc, columnAlias, fieldName = field.getName();
                boolean primary;
                int strLen;
                if (columnInfo != null) {
                    if (columnInfo.ignore()) {
                        continue;
                    }

                    columnName = columnInfo.value();
                    columnDesc = columnInfo.desc();
                    columnAlias = defaultIfBlank(columnInfo.alias(), columnName);
                    primary = columnInfo.primary();
                    strLen = columnInfo.varcharLength();

                    // 用类名 + 列名
                    String schemaAndColumn = schemaName + "." + columnName;
                    columnInfoMap.put(schemaAndColumn, columnInfo);
                    columnClassMap.put(schemaAndColumn, fieldType);
                } else {
                    columnDesc = "";
                    columnAlias = field.getName();
                    columnName = aliasToColumnName(columnAlias);
                    primary = "id".equalsIgnoreCase(columnAlias);
                    strLen = 0;
                }

                if (columnNameSet.contains(columnName)) {
                    throw new RuntimeException("schema(" + schemaAlias + ") has same column(" + columnName + ")");
                }
                columnNameSet.add(columnName);
                if (columnAliasSet.contains(columnAlias)) {
                    throw new RuntimeException("schema(" + schemaAlias + ") has same column(" + columnAlias + ")");
                }
                columnAliasSet.add(columnAlias);

                aliasMap.put(QueryConst.COLUMN_PREFIX + columnAlias, columnName);
                columnMap.put(columnName, new SchemaColumn(columnName, columnDesc,
                        columnAlias, primary, strLen, fieldType, fieldName));
            }
            aliasMap.put(QueryConst.SCHEMA_PREFIX + schemaAlias, schemaName);
            schemaClassMap.put(clazz.getName(), schemaName);
            schemaMap.put(schemaName, new Schema(schemaName, schemaDesc, schemaAlias, columnMap));
        }

        for (Map.Entry<String, ColumnInfo> entry : columnInfoMap.entrySet()) {
            ColumnInfo columnInfo = entry.getValue();
            SchemaRelationType relationType = columnInfo.relationType();
            if (relationType != SchemaRelationType.NULL) {
                String oneSchema = columnInfo.relationSchema();
                String oneColumn = columnInfo.relationColumn();
                if (!oneSchema.isEmpty() && !oneColumn.isEmpty()) {
                    String schemaAndColumn = entry.getKey();
                    Schema schema = schemaMap.get(aliasMap.get(QueryConst.SCHEMA_PREFIX + oneSchema));
                    if (schema == null) {
                        schema = schemaMap.get(oneSchema);
                        if (schema == null) {
                            throw new RuntimeException(schemaAndColumn + "'s relation no schema(" + oneSchema + ")");
                        }
                    }

                    Map<String, SchemaColumn> columnMap = schema.getColumnMap();
                    SchemaColumn column = columnMap.get(aliasMap.get(QueryConst.COLUMN_PREFIX + oneColumn));
                    if (column == null) {
                        column = columnMap.get(oneColumn);
                        if (column == null) {
                            throw new RuntimeException(schemaAndColumn + "'s relation no schema-column("
                                    + oneSchema + "." + oneColumn + ")");
                        }
                    }
                    Class<?> sourceClass = columnClassMap.get(schemaAndColumn);
                    Class<?> targetClass = column.getColumnType();
                    if (sourceClass != targetClass) {
                        throw new RuntimeException(schemaAndColumn + "'s data type has " + sourceClass.getSimpleName()
                                + ", but relation " + oneSchema + "'s data type has" + targetClass.getSimpleName());
                    }
                    // 用列名, 不是别名
                    String[] arr = schemaAndColumn.split("\\.");
                    String relationSchema = arr[0];
                    String relationColumn = arr[1];
                    relationList.add(new SchemaColumnRelation(schema.getName(), column.getName(),
                            relationType, relationSchema, relationColumn));
                }
            }
        }
        return new SchemaColumnInfo(aliasMap, schemaClassMap, schemaMap, relationList);
    }
    /** UserInfo --> user_info */
    private static String aliasToSchemaName(String className) {
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
        return sbd.toString();
    }
    /** userName --> user_name */
    private static String aliasToColumnName(String fieldName) {
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
    public static String schemaNameToAlias(String schemaName) {
        if (schemaName.toLowerCase().startsWith("t_")) {
            schemaName = schemaName.substring(2);
        }
        StringBuilder sbd = new StringBuilder();
        char[] chars = schemaName.toCharArray();
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
        for (String format : QueryConst.DATE_FORMAT_LIST) {
            try {
                Date date = new SimpleDateFormat(format).parse(obj.toString().trim());
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


    public static String getSchemaName(String column, String mainSchema) {
        return column.contains(".") ? column.split("\\.")[0].trim() : mainSchema;
    }

    public static String getColumnName(String column) {
        return column.contains(".") ? column.split("\\.")[1].trim() : column.trim();
    }

    public static String getUseColumn(boolean needAlias, String column, String mainSchema, SchemaColumnInfo scInfo) {
        String schemaName = getSchemaName(column, mainSchema);
        String columnName = getColumnName(column);
        Schema schema = scInfo.findSchema(schemaName);
        SchemaColumn schemaColumn = scInfo.findSchemaColumn(schema, columnName);
        String useColumnName = QuerySqlUtil.toSqlField(schemaColumn.getName());
        if (needAlias) {
            String alias = schema.getAlias();
            return QuerySqlUtil.toSqlField(alias) + "." + useColumnName + " AS " + alias + "_" + schemaColumn.getName();
        } else {
            return useColumnName;
        }
    }

    public static String getUseQueryColumn(boolean needAlias, String column, String mainSchema, SchemaColumnInfo scInfo) {
        String schemaName = getSchemaName(column, mainSchema);
        String columnName = getColumnName(column);
        Schema schema = scInfo.findSchema(schemaName);
        SchemaColumn schemaColumn = scInfo.findSchemaColumn(schema, columnName);
        String schemaColumnName = schemaColumn.getName();
        String schemaColumnAlias = schemaColumn.getAlias();
        String useColumnName = QuerySqlUtil.toSqlField(schemaColumnName);
        if (needAlias) {
            String alias = schema.getAlias();
            return QuerySqlUtil.toSqlField(alias) + "." + useColumnName + " AS " + alias + "_" + schemaColumnAlias;
        } else {
            return useColumnName + (schemaColumnName.equals(schemaColumnAlias) ? "" : (" AS " + schemaColumnAlias));
        }
    }
}
