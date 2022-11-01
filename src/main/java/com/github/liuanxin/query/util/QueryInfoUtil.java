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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

@SuppressWarnings("DuplicatedCode")
public class QueryInfoUtil {

    private static final Logger LOG = LoggerFactory.getLogger(QueryInfoUtil.class);

    private static final PathMatchingResourcePatternResolver RESOLVER =
            new PathMatchingResourcePatternResolver(ClassLoader.getSystemClassLoader());

    private static final MetadataReaderFactory READER = new CachingMetadataReaderFactory(RESOLVER);


    public static TableColumnInfo infoWithScan(String tablePrefix, String classPackages,
                                               String globalLogicColumn, String globalLogicValue,
                                               String globalLogicDeleteBooleanValue, String globalLogicDeleteIntValue,
                                               String globalLogicDeleteLongValue) {
        return infoWithClass(tablePrefix, scanPackage(classPackages),
                globalLogicColumn, globalLogicValue, globalLogicDeleteBooleanValue,
                globalLogicDeleteIntValue, globalLogicDeleteLongValue);
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

    private static TableColumnInfo infoWithClass(String tablePrefix, Set<Class<?>> classes,
                                                 String globalLogicColumn, String globalLogicValue,
                                                 String globalLogicDeleteBooleanValue, String globalLogicDeleteIntValue,
                                                 String globalLogicDeleteLongValue) {
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
                boolean notNull, hasDefault;
                if (QueryUtil.isNotNull(columnInfo)) {
                    if (columnInfo.ignore()) {
                        continue;
                    }

                    columnName = QueryUtil.defaultIfBlank(columnInfo.value(), QueryUtil.fieldToColumnName(fieldName));

                    String columnLogicValue = columnInfo.logicValue();
                    String columnLogicDeleteValue = columnInfo.logicDeleteValue();
                    if (QueryUtil.isNotEmpty(columnLogicValue) && QueryUtil.isNotEmpty(columnLogicDeleteValue)) {
                        if (QueryUtil.isNotEmpty(logicColumn)) {
                            throw new RuntimeException("table(" + tableAlias + ") can only has one column with logic delete");
                        }
                        logicColumn = columnName;
                        logicValue = columnLogicValue;
                        logicDeleteValue = columnLogicDeleteValue;
                    }

                    columnDesc = columnInfo.desc();
                    // 1. alias, 2. column-name, 3. field-name
                    columnAlias = QueryUtil.defaultIfBlank(QueryUtil.defaultIfBlank(columnInfo.alias(), columnName), fieldName);
                    primary = columnInfo.primary();
                    strLen = columnInfo.varcharLength();
                    notNull = columnInfo.notNull();
                    hasDefault = columnInfo.hasDefault();

                    String tableAndColumn = tableName + "." + columnName;
                    columnInfoMap.put(tableAndColumn, columnInfo);
                    columnClassMap.put(tableAndColumn, fieldType);
                } else {
                    columnDesc = "";
                    columnAlias = field.getName();
                    columnName = QueryUtil.fieldToColumnName(columnAlias);
                    primary = "id".equalsIgnoreCase(columnAlias);
                    strLen = null;
                    notNull = false;
                    hasDefault = false;
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
                        ((strLen == null || strLen <= 0) ? null : strLen), notNull, hasDefault, fieldType, fieldName));
            }
            if (QueryUtil.isEmpty(logicColumn)) {
                // noinspection DuplicatedCode
                TableColumn tableColumn = columnMap.get(globalLogicColumn);
                if (QueryUtil.isNotNull(tableColumn)) {
                    Class<?> fieldType = tableColumn.getFieldType();
                    if (fieldType == Boolean.class || fieldType == boolean.class) {
                        logicDeleteValue = globalLogicDeleteBooleanValue;
                    } else if (fieldType == Integer.class || fieldType == int.class) {
                        logicDeleteValue = globalLogicDeleteIntValue;
                    } else if (fieldType == Long.class || fieldType == long.class) {
                        logicDeleteValue = globalLogicDeleteLongValue;
                    }
                    if (QueryUtil.isNotNull(logicDeleteValue)) {
                        logicColumn = globalLogicColumn;
                        logicValue = globalLogicValue;
                    }
                }
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
                    Class<?> targetClass = column.getFieldType();
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
                                             List<Map<String, Object>> indexList,
                                             String globalLogicColumn, String globalLogicValue,
                                             String globalLogicDeleteBooleanValue, String globalLogicDeleteIntValue,
                                             String globalLogicDeleteLongValue) {
        Map<String, String> aliasMap = new HashMap<>();
        Map<String, Table> tableMap = new LinkedHashMap<>();
        List<TableColumnRelation> relationList = new ArrayList<>();

        Map<String, List<Map<String, Object>>> tableColumnMap = new HashMap<>();
        tableColumnListToMap(tableColumnList, tableColumnMap);

        Map<String, Map<String, Map<String, Object>>> relationColumnMap = new HashMap<>();
        relationColumnListToMap(relationColumnList, relationColumnMap);

        Map<String, Set<String>> columnUniqueMap = new HashMap<>();
        columnUniqueListToMap(indexList, columnUniqueMap);

        for (Map<String, Object> tableInfo : tableList) {
            String tableName = QueryUtil.toStr(tableInfo.get("tn"));
            String tableAlias = QueryUtil.tableNameToClass(tablePrefix, tableName);
            String tableDesc = QueryUtil.toStr(tableInfo.get("tc"));
            Map<String, TableColumn> columnMap = new LinkedHashMap<>();
            for (Map<String, Object> columnInfo : tableColumnMap.get(tableName)) {
                Class<?> fieldType = QueryUtil.mappingClass(QueryUtil.toStr(columnInfo.get("ct")));
                String columnName = QueryUtil.toStr(columnInfo.get("cn"));
                String fieldName = QueryUtil.columnNameToField(columnName);
                String columnAlias = QueryUtil.defaultIfBlank(fieldName, columnName);
                String columnDesc = QueryUtil.toStr(columnInfo.get("cc"));
                boolean primary = "PRI".equalsIgnoreCase(QueryUtil.toStr(columnInfo.get("ck")));
                Integer strLen = QueryUtil.toInteger(QueryUtil.toStr(columnInfo.get("cml")));
                boolean notNull = QueryUtil.toBool(QueryUtil.toStr(columnInfo.get("ine")));
                boolean primaryIncrement = primary && "auto_increment".equalsIgnoreCase(QueryUtil.toStr(columnInfo.get("ex")));
                boolean hasDefault = primaryIncrement || QueryUtil.isNotNull(columnInfo.get("cd"));

                aliasMap.put(QueryConst.COLUMN_PREFIX + columnAlias, columnName);
                columnMap.put(columnName, new TableColumn(columnName, columnDesc, columnAlias, primary,
                        ((strLen == null || strLen <= 0) ? null : strLen), notNull, hasDefault, fieldType, fieldName));
            }
            String logicColumn = null, logicValue = null, logicDeleteValue = null;
            // noinspection DuplicatedCode
            TableColumn tableColumn = columnMap.get(globalLogicColumn);
            if (QueryUtil.isNotNull(tableColumn)) {
                Class<?> fieldType = tableColumn.getFieldType();
                if (fieldType == Boolean.class || fieldType == boolean.class) {
                    logicDeleteValue = globalLogicDeleteBooleanValue;
                } else if (fieldType == Integer.class || fieldType == int.class) {
                    logicDeleteValue = globalLogicDeleteIntValue;
                } else if (fieldType == Long.class || fieldType == long.class) {
                    logicDeleteValue = globalLogicDeleteLongValue;
                }
                if (QueryUtil.isNotNull(logicDeleteValue)) {
                    logicColumn = globalLogicColumn;
                    logicValue = globalLogicValue;
                }
            }
            aliasMap.put(QueryConst.TABLE_PREFIX + tableAlias, tableName);
            tableMap.put(tableName, new Table(tableName, tableDesc, tableAlias,
                    logicColumn, logicValue, logicDeleteValue, columnMap));
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

    private static void tableColumnListToMap(List<Map<String, Object>> tableColumnList,
                                             Map<String, List<Map<String, Object>>> tableColumnMap) {
        if (!tableColumnList.isEmpty()) {
            for (Map<String, Object> tableColumn : tableColumnList) {
                String key = QueryUtil.toStr(tableColumn.get("tn"));
                tableColumnMap.computeIfAbsent(key, (k) -> new ArrayList<>()).add(tableColumn);
            }
        }
    }
    private static void relationColumnListToMap(List<Map<String, Object>> relationColumnList,
                                                Map<String, Map<String, Map<String, Object>>> relationColumnMap) {
        if (!relationColumnList.isEmpty()) {
            for (Map<String, Object> relationColumn : relationColumnList) {
                String tableName = QueryUtil.toStr(relationColumn.get("tn"));
                Map<String, Map<String, Object>> columnMap = relationColumnMap.getOrDefault(tableName, new HashMap<>());
                columnMap.put(QueryUtil.toStr(relationColumn.get("cn")), relationColumn);
                relationColumnMap.put(tableName, columnMap);
            }
        }
    }
    private static void columnUniqueListToMap(List<Map<String, Object>> indexList,
                                              Map<String, Set<String>> columnUniqueMap) {
        if (!indexList.isEmpty()) {
            for (Map<String, Object> index : indexList) {
                String tableName = QueryUtil.toStr(index.get("tn"));
                Set<String> uniqueColumnSet = columnUniqueMap.getOrDefault(tableName, new HashSet<>());
                uniqueColumnSet.add(QueryUtil.toStr(index.get("cn")));
                columnUniqueMap.put(tableName, uniqueColumnSet);
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void generateModel(Set<String> tableSet, String targetPath, String packagePath, String tablePrefix,
                                     List<Map<String, Object>> tableList, List<Map<String, Object>> tableColumnList,
                                     List<Map<String, Object>> relationColumnList, List<Map<String, Object>> indexList) {
        File dir = new File(targetPath.replace(".", "/"));
        deleteDirectory(dir);

        Map<String, List<Map<String, Object>>> tableColumnMap = new HashMap<>();
        tableColumnListToMap(tableColumnList, tableColumnMap);

        Map<String, Map<String, Map<String, Object>>> relationColumnMap = new HashMap<>();
        relationColumnListToMap(relationColumnList, relationColumnMap);

        Map<String, Set<String>> columnUniqueMap = new HashMap<>();
        columnUniqueListToMap(indexList, columnUniqueMap);

        Map<String, TableColumnRelation> relationMap = new HashMap<>();
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

                    relationMap.put(relationTable + "<->" + relationColumn,
                            new TableColumnRelation(oneTable, oneColumn, type, relationTable, relationColumn));
                }
            }
        }

        StringBuilder sbd = new StringBuilder();
        Set<String> importSet = new TreeSet<>();
        Set<String> javaImportSet = new TreeSet<>();
        for (Map<String, Object> tableInfo : tableList) {
            String tableName = QueryUtil.toStr(tableInfo.get("tn"));
            if (QueryUtil.isNotEmpty(tableSet) && !tableSet.contains(tableName.toLowerCase())) {
                continue;
            }
            sbd.setLength(0);
            importSet.clear();
            javaImportSet.clear();
            String className = QueryUtil.tableNameToClass(tablePrefix, tableName);
            String tableDesc = QueryUtil.toStr(tableInfo.get("tc"));

            List<String> fieldList = new ArrayList<>();
            for (Map<String, Object> columnInfo : tableColumnMap.get(tableName)) {
                Class<?> fieldType = QueryUtil.mappingClass(QueryUtil.toStr(columnInfo.get("ct")));
                String columnName = QueryUtil.toStr(columnInfo.get("cn"));
                String fieldName = QueryUtil.columnNameToField(columnName);
                String columnDesc = QueryUtil.toStr(columnInfo.get("cc"));
                boolean primary = "PRI".equalsIgnoreCase(QueryUtil.toStr(columnInfo.get("ck")));
                Integer strLen = QueryUtil.toInteger(QueryUtil.toStr(columnInfo.get("cml")));
                boolean hasLen = (strLen != null && strLen > 0);
                boolean notNull = QueryUtil.toBool(QueryUtil.toStr(columnInfo.get("ine")));
                boolean primaryIncrement = primary && "auto_increment".equalsIgnoreCase(QueryUtil.toStr(columnInfo.get("ex")));
                boolean hasDefault = primaryIncrement || QueryUtil.isNotNull(columnInfo.get("cd"));

                StringBuilder fieldSbd = new StringBuilder();
                if (QueryUtil.isNotEmpty(columnDesc)) {
                    fieldSbd.append(space(4)).append(String.format("/** %s --> %s */\n", columnDesc, columnName));
                }
                importSet.add("import " + ColumnInfo.class.getName() + ";");

                fieldSbd.append(space(4));
                fieldSbd.append(String.format("@ColumnInfo(value = \"%s\"", columnName));
                if (QueryUtil.isNotEmpty(columnDesc)) {
                    fieldSbd.append(String.format(", desc = \"%s\"", columnDesc));
                }
                if (primary) {
                    fieldSbd.append(", primary = true");
                }
                if (hasLen) {
                    fieldSbd.append(", varcharLength = ").append(strLen);
                }
                if (notNull) {
                    fieldSbd.append(", notNull = true");
                }
                if (hasDefault) {
                    fieldSbd.append(", hasDefault = true");
                }
                TableColumnRelation relation = relationMap.get(tableName + "<->" + columnName);
                if (QueryUtil.isNotNull(relation)) {
                    importSet.add("import " + TableRelationType.class.getName() + ";");
                    fieldSbd.append(",\n").append(space(12)).append("relationType = ");
                    TableRelationType relationType = relation.getType();
                    fieldSbd.append(relationType.getClass().getSimpleName()).append(".").append(relationType.name());
                    fieldSbd.append(", relationTable = \"").append(relation.getOneTable()).append("\"");
                    fieldSbd.append(", relationColumn = \"").append(relation.getOneColumn()).append("\"");
                }
                fieldSbd.append(")\n");
                fieldSbd.append(space(4)).append("private ");
                String classType = fieldType.getName();
                if (!classType.startsWith("java.lang")) {
                    javaImportSet.add("import " + classType + ";");
                }
                fieldSbd.append(fieldType.getSimpleName()).append(" ").append(fieldName).append(";\n");
                fieldList.add(fieldSbd.toString());
            }
            importSet.add("import lombok.Data;");
            importSet.add("import " + TableInfo.class.getName() + ";");
            sbd.append("@Data\n");
            sbd.append(String.format("@TableInfo(value = \"%s\", desc = \"%s\")\n", tableName, tableDesc));
            sbd.append("public class ").append(className).append(" {\n\n");
            sbd.append(String.join("\n", fieldList));
            sbd.append("}\n");
            sbd.insert(0, "package " + packagePath.replace("/", ".") + ";\n\n"
                    + String.join("\n", importSet) + "\n\n"
                    + String.join("\n", javaImportSet) + "\n\n");
            File packageDir = new File(dir, packagePath.replace(".", "/"));
            if (!packageDir.exists()) {
                packageDir.mkdirs();
            }
            File file = new File(packageDir, className + ".java");
            try {
                Files.write(file.toPath(), sbd.toString().getBytes(StandardCharsets.UTF_8));
                if (LOG.isInfoEnabled()) {
                    LOG.info("file({}) write success", file);
                }
            } catch (IOException e) {
                throw new RuntimeException(String.format("generate file(%s) exception", file), e);
            }
        }
    }
    private static String space(int count) {
        StringBuilder sbd = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sbd.append(" ");
        }
        return sbd.toString();
    }

    private static void deleteDirectory(File f) {
        if (f.exists()) {
            if (f.isDirectory()) {
                File[] files = f.listFiles();
                if (files != null && files.length > 0) {
                    for (File file : files) {
                        deleteDirectory(file);
                    }
                }
                boolean flag = f.delete();
                if (flag) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("directory({}) delete success", f);
                    }
                } else {
                    throw new RuntimeException(String.format("directory(%s) delete fail", f));
                }
            } else {
                boolean flag = f.delete();
                if (flag) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("file({}) delete success", f);
                    }
                } else {
                    throw new RuntimeException(String.format("file(%s) delete fail", f));
                }
            }
        }
    }
}
