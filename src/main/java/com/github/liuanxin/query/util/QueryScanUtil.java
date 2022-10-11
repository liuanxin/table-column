package com.github.liuanxin.query.util;

import com.github.liuanxin.query.annotation.ColumnInfo;
import com.github.liuanxin.query.annotation.TableInfo;
import com.github.liuanxin.query.constant.QueryConst;
import com.github.liuanxin.query.enums.TableRelationType;
import com.github.liuanxin.query.model.Table;
import com.github.liuanxin.query.model.TableColumn;
import com.github.liuanxin.query.model.TableColumnInfo;
import com.github.liuanxin.query.model.TableColumnRelation;
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


    public static TableColumnInfo scanTable(String classPackages) {
        return handleTable(scanPackage(classPackages));
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

    private static TableColumnInfo handleTable(Set<Class<?>> classes) {
        Map<String, String> aliasMap = new HashMap<>();
        Map<String, String> tableClassMap = new HashMap<>();
        Map<String, Table> tableMap = new LinkedHashMap<>();
        List<TableColumnRelation> relationList = new ArrayList<>();

        Map<String, ColumnInfo> columnInfoMap = new LinkedHashMap<>();
        Map<String, Class<?>> columnClassMap = new HashMap<>();
        Set<String> tableNameSet = new HashSet<>();
        Set<String> tableAliasSet = new HashSet<>();
        Set<String> columnNameSet = new HashSet<>();
        Set<String> columnAliasSet = new HashSet<>();
        for (Class<?> clazz : classes) {
            TableInfo tableInfo = clazz.getAnnotation(TableInfo.class);
            String tableName, tableDesc, tableAlias;
            if (tableInfo != null) {
                if (tableInfo.ignore()) {
                    continue;
                }

                tableName = tableInfo.value();
                tableDesc = tableInfo.desc();
                tableAlias = QueryUtil.defaultIfBlank(tableInfo.alias(), tableName);
            } else {
                tableDesc = "";
                tableAlias = clazz.getSimpleName();
                tableName = QueryUtil.aliasToTableName(tableAlias);
            }

            if (tableNameSet.contains(tableName)) {
                throw new RuntimeException("table(" + tableName + ") has renamed");
            }
            tableNameSet.add(tableName);
            if (tableAliasSet.contains(tableAlias)) {
                throw new RuntimeException("table alias(" + tableName + ") has renamed");
            }
            tableAliasSet.add(tableAlias);

            Map<String, TableColumn> columnMap = new LinkedHashMap<>();
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
                    strLen = columnInfo.varcharLength();

                    // 用类名 + 列名
                    String tableAndColumn = tableName + "." + columnName;
                    columnInfoMap.put(tableAndColumn, columnInfo);
                    columnClassMap.put(tableAndColumn, fieldType);
                } else {
                    columnDesc = "";
                    columnAlias = field.getName();
                    columnName = QueryUtil.aliasToColumnName(columnAlias);
                    primary = "id".equalsIgnoreCase(columnAlias);
                    strLen = null;
                }

                if (columnNameSet.contains(columnName)) {
                    throw new RuntimeException("table(" + tableAlias + ") has same column(" + columnName + ")");
                }
                columnNameSet.add(columnName);
                if (columnAliasSet.contains(columnAlias)) {
                    throw new RuntimeException("table(" + tableAlias + ") has same column(" + columnAlias + ")");
                }
                columnAliasSet.add(columnAlias);

                aliasMap.put(QueryConst.COLUMN_PREFIX + columnAlias, columnName);
                columnMap.put(columnName, new TableColumn(columnName, columnDesc, columnAlias, primary,
                        ((strLen == null || strLen <= 0) ? null : strLen), fieldType, fieldName));
            }
            aliasMap.put(QueryConst.TABLE_PREFIX + tableAlias, tableName);
            tableClassMap.put(clazz.getName(), tableName);
            tableMap.put(tableName, new Table(tableName, tableDesc, tableAlias, columnMap));
        }

        for (Map.Entry<String, ColumnInfo> entry : columnInfoMap.entrySet()) {
            ColumnInfo columnInfo = entry.getValue();
            TableRelationType relationType = columnInfo.relationType();
            if (relationType != TableRelationType.NULL) {
                String oneTable = columnInfo.relationTable();
                String oneColumn = columnInfo.relationColumn();
                if (!oneTable.isEmpty() && !oneColumn.isEmpty()) {
                    String tableAndColumn = entry.getKey();
                    Table table = tableMap.get(aliasMap.get(QueryConst.TABLE_PREFIX + oneTable));
                    if (table == null) {
                        table = tableMap.get(oneTable);
                        if (table == null) {
                            throw new RuntimeException(tableAndColumn + "'s relation no table(" + oneTable + ")");
                        }
                    }

                    Map<String, TableColumn> columnMap = table.getColumnMap();
                    TableColumn column = columnMap.get(aliasMap.get(QueryConst.COLUMN_PREFIX + oneColumn));
                    if (column == null) {
                        column = columnMap.get(oneColumn);
                        if (column == null) {
                            throw new RuntimeException(tableAndColumn + "'s relation no table-column("
                                    + oneTable + "." + oneColumn + ")");
                        }
                    }
                    Class<?> sourceClass = columnClassMap.get(tableAndColumn);
                    Class<?> targetClass = column.getColumnType();
                    if (sourceClass != targetClass) {
                        throw new RuntimeException(tableAndColumn + "'s data type has " + sourceClass.getSimpleName()
                                + ", but relation " + oneTable + "'s data type has" + targetClass.getSimpleName());
                    }
                    String[] arr = tableAndColumn.split("\\.");
                    String relationTable = arr[0];
                    String relationColumn = arr[1];
                    relationList.add(new TableColumnRelation(table.getName(), column.getName(),
                            relationType, relationTable, relationColumn));
                }
            }
        }
        return new TableColumnInfo(aliasMap, tableClassMap, tableMap, relationList);
    }
}
