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

public class QueryInfoUtil {

    private static final Logger LOG = LoggerFactory.getLogger(QueryInfoUtil.class);

    private static final PathMatchingResourcePatternResolver RESOLVER =
            new PathMatchingResourcePatternResolver(ClassLoader.getSystemClassLoader());

    private static final MetadataReaderFactory READER = new CachingMetadataReaderFactory(RESOLVER);


    public static TableColumnInfo infoWithScan(String tablePrefix, String classPackages) {
        return infoWithClass(tablePrefix, scanPackage(classPackages));
    }

    private static Set<Class<?>> scanPackage(String classPackages) {
        if (QueryUtil.isNull(classPackages)) {
            return Collections.emptySet();
        }

        Set<Class<?>> set = new LinkedHashSet<>();
        for (String cp : classPackages.split(",")) {
            String path = QueryUtil.isNull(cp) ? null : cp.trim();
            if (QueryUtil.isNotEmpty(path)) {
                try {
                    String location = String.format("classpath*:**/%s/**/*.class", path.replace(".", "/"));
                    for (Resource resource : RESOLVER.getResources(location)) {
                        if (resource.isReadable()) {
                            set.add(Class.forName(READER.getMetadataReader(resource).getClassMetadata().getClassName()));
                        }
                    }
                } catch (Exception e) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("get({}) class exception", path, e);
                    }
                }
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("scan {} class", set);
        }
        return set;
    }

    private static TableColumnInfo infoWithClass(String tablePrefix, Set<Class<?>> classes) {
        if (classes.isEmpty()) {
            return null;
        }

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
            if (QueryUtil.isNotNull(tableInfo)) {
                if (tableInfo.ignore()) {
                    continue;
                }

                tableName = tableInfo.value();
                tableDesc = tableInfo.desc();
                tableAlias = QueryUtil.defaultIfBlank(tableInfo.alias(), tableName);
            } else {
                tableDesc = "";
                tableAlias = clazz.getSimpleName();
                tableName = QueryUtil.classToTableName(tablePrefix, tableAlias);
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
            String logicColumn = "", logicValue = "", logicDeleteValue = "";
            for (Field field : QueryUtil.getFields(clazz)) {
                ColumnInfo columnInfo = field.getAnnotation(ColumnInfo.class);
                Class<?> fieldType = field.getType();
                String columnName, columnDesc, columnAlias, fieldName = field.getName();
                boolean primary;
                Integer strLen;
                if (QueryUtil.isNotNull(columnInfo)) {
                    if (columnInfo.ignore()) {
                        continue;
                    }

                    String columnLogicValue = columnInfo.logicValue();
                    String columnLogicDeleteValue = columnInfo.logicDeleteValue();
                    if (QueryUtil.isNotEmpty(columnLogicValue) && QueryUtil.isNotEmpty(columnLogicDeleteValue)) {
                        if (QueryUtil.isNotEmpty(logicColumn)) {
                            throw new RuntimeException("table(" + tableAlias + ") can only has one column with logic delete");
                        }
                        logicColumn = columnInfo.value();
                        logicValue = columnLogicValue;
                        logicDeleteValue = columnLogicDeleteValue;
                    }

                    columnName = columnInfo.value();
                    columnDesc = columnInfo.desc();
                    columnAlias = QueryUtil.defaultIfBlank(columnInfo.alias(), columnName);
                    primary = columnInfo.primary();
                    strLen = columnInfo.varcharLength();

                    String tableAndColumn = tableName + "." + columnName;
                    columnInfoMap.put(tableAndColumn, columnInfo);
                    columnClassMap.put(tableAndColumn, fieldType);
                } else {
                    columnDesc = "";
                    columnAlias = field.getName();
                    columnName = QueryUtil.fieldToColumnName(columnAlias);
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
            tableMap.put(tableName, new Table(tableName, tableDesc, tableAlias,
                    logicColumn, logicValue, logicDeleteValue, columnMap));
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
                    if (QueryUtil.isNull(table)) {
                        table = tableMap.get(oneTable);
                    }
                    if (QueryUtil.isNull(table)) {
                        throw new RuntimeException(tableAndColumn + "'s relation no table(" + oneTable + ")");
                    }

                    Map<String, TableColumn> columnMap = table.getColumnMap();
                    TableColumn column = columnMap.get(aliasMap.get(QueryConst.COLUMN_PREFIX + oneColumn));
                    if (QueryUtil.isNull(column)) {
                        column = columnMap.get(oneColumn);
                    }
                    if (QueryUtil.isNull(column)) {
                        throw new RuntimeException(tableAndColumn + "'s relation no table-column("
                                + oneTable + "." + oneColumn + ")");
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


    public static TableColumnInfo infoWithDb(String tablePrefix,
                                             List<Map<String, Object>> tableList,
                                             List<Map<String, Object>> tableColumnList,
                                             List<Map<String, Object>> relationColumnList,
                                             List<Map<String, Object>> indexList) {
        Map<String, String> aliasMap = new HashMap<>();
        Map<String, Table> tableMap = new LinkedHashMap<>();
        List<TableColumnRelation> relationList = new ArrayList<>();

        Map<String, List<Map<String, Object>>> tableColumnMap = new HashMap<>();
        if (!tableColumnList.isEmpty()) {
            for (Map<String, Object> tableColumn : tableColumnList) {
                String key = QueryUtil.toStr(tableColumn.get("tn"));
                tableColumnMap.computeIfAbsent(key, (k1) -> new ArrayList<>()).add(tableColumn);
            }
        }
        Map<String, Map<String, Map<String, Object>>> relationColumnMap = new HashMap<>();
        if (!relationColumnList.isEmpty()) {
            for (Map<String, Object> relationColumn : relationColumnList) {
                String tableName = QueryUtil.toStr(relationColumn.get("tn"));
                Map<String, Map<String, Object>> columnMap = relationColumnMap.getOrDefault(tableName, new HashMap<>());
                columnMap.put(QueryUtil.toStr(relationColumn.get("cn")), relationColumn);
                relationColumnMap.put(tableName, columnMap);
            }
        }
        Map<String, Set<String>> columnUniqueMap = new HashMap<>();
        if (!indexList.isEmpty()) {
            for (Map<String, Object> index : indexList) {
                String tableName = QueryUtil.toStr(index.get("tn"));
                Set<String> uniqueColumnSet = columnUniqueMap.getOrDefault(tableName, new HashSet<>());
                uniqueColumnSet.add(QueryUtil.toStr(index.get("cn")));
                columnUniqueMap.put(tableName, uniqueColumnSet);
            }
        }

        for (Map<String, Object> tableInfo : tableList) {
            String tableName = QueryUtil.toStr(tableInfo.get("tn"));
            String tableAlias = QueryUtil.tableNameToClass(tablePrefix, tableName);
            String tableDesc = QueryUtil.toStr(tableInfo.get("tc"));
            Map<String, TableColumn> columnMap = new LinkedHashMap<>();

            List<Map<String, Object>> columnList = tableColumnMap.get(tableName);
            for (Map<String, Object> columnInfo : columnList) {
                Class<?> fieldType = QueryUtil.mappingClass(QueryUtil.toStr(columnInfo.get("ct")));
                String columnName = QueryUtil.toStr(columnInfo.get("cn"));
                String columnAlias = QueryUtil.columnNameToField(columnName);
                String columnDesc = QueryUtil.toStr(columnInfo.get("cc"));
                boolean primary = "PRI".equalsIgnoreCase(QueryUtil.toStr(columnInfo.get("ck")));
                Integer strLen = QueryUtil.toInteger(QueryUtil.toStr(columnInfo.get("cml")));

                aliasMap.put(QueryConst.COLUMN_PREFIX + columnName, columnAlias);
                columnMap.put(columnAlias, new TableColumn(columnName, columnDesc, columnAlias, primary,
                        ((strLen == null || strLen <= 0) ? null : strLen), fieldType, columnAlias));
            }
            aliasMap.put(QueryConst.TABLE_PREFIX + tableName, tableAlias);
            tableMap.put(tableAlias, new Table(tableName, tableDesc, tableAlias, null, null, null, columnMap));
        }

        if (!relationColumnMap.isEmpty()) {
            for (Map.Entry<String, Map<String, Map<String, Object>>> entry : relationColumnMap.entrySet()) {
                String relationTable = entry.getKey();
                Set<String> uniqueColumnSet = columnUniqueMap.get(relationTable);
                for (Map.Entry<String, Map<String, Object>> columnEntry : entry.getValue().entrySet()) {
                    String relationColumn = columnEntry.getKey();
                    TableRelationType type = uniqueColumnSet.contains(relationColumn)
                            ? TableRelationType.ONE_TO_ONE : TableRelationType.ONE_TO_MANY;

                    Map<String, Object> relationInfoMap = columnEntry.getValue();
                    String oneTable = QueryUtil.toStr(relationInfoMap.get("ftn"));
                    String oneColumn = QueryUtil.toStr(relationInfoMap.get("fcn"));

                    relationList.add(new TableColumnRelation(oneTable, oneColumn, type, relationTable, relationColumn));
                }
            }
        }
        return new TableColumnInfo(aliasMap, new HashMap<>(), tableMap, relationList);
    }
}
