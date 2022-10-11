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
import java.util.*;

public class QueryScanUtil {

    private static final Logger LOG = LoggerFactory.getLogger(QueryScanUtil.class);

    private static final PathMatchingResourcePatternResolver RESOLVER =
            new PathMatchingResourcePatternResolver(ClassLoader.getSystemClassLoader());

    private static final MetadataReaderFactory READER = new CachingMetadataReaderFactory(RESOLVER);


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
                schemaAlias = QueryUtil.defaultIfBlank(schemaInfo.alias(), schemaName);
            } else {
                schemaDesc = "";
                schemaAlias = clazz.getSimpleName();
                schemaName = QueryUtil.aliasToSchemaName(schemaAlias);
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
            for (Field field : QueryUtil.getFields(clazz)) {
                ColumnInfo columnInfo = field.getAnnotation(ColumnInfo.class);
                Class<?> fieldType = field.getType();
                String columnName, columnDesc, columnAlias, fieldName = field.getName();
                boolean primary;
                Integer strLen;
                if (columnInfo != null) {
                    if (columnInfo.ignore()) {
                        continue;
                    }

                    columnName = columnInfo.value();
                    columnDesc = columnInfo.desc();
                    columnAlias = QueryUtil.defaultIfBlank(columnInfo.alias(), columnName);
                    primary = columnInfo.primary();
                    strLen = (columnInfo.varcharLength() <= 0) ? null : columnInfo.varcharLength();

                    // 用类名 + 列名
                    String schemaAndColumn = schemaName + "." + columnName;
                    columnInfoMap.put(schemaAndColumn, columnInfo);
                    columnClassMap.put(schemaAndColumn, fieldType);
                } else {
                    columnDesc = "";
                    columnAlias = field.getName();
                    columnName = QueryUtil.aliasToColumnName(columnAlias);
                    primary = "id".equalsIgnoreCase(columnAlias);
                    strLen = null;
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
}
