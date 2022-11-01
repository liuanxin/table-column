package com.github.liuanxin.query.util;

import com.github.liuanxin.query.annotation.*;
import com.github.liuanxin.query.constant.QueryConst;
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
                                               List<TableColumnRelation> relationList,
                                               String globalLogicColumn, String globalLogicValue,
                                               String globalLogicDeleteBooleanValue, String globalLogicDeleteIntValue,
                                               String globalLogicDeleteLongValue) {
        return infoWithClass(tablePrefix, scanPackage(classPackages), relationList,
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
                                                 List<TableColumnRelation> relationList,
                                                 String globalLogicColumn, String globalLogicValue,
                                                 String globalLogicDeleteBooleanValue, String globalLogicDeleteIntValue,
                                                 String globalLogicDeleteLongValue) {
        if (QueryUtil.isEmpty(classes)) {
            return null;
        }

        Map<String, String> aliasMap = new HashMap<>();
        Map<String, String> tableClassMap = new HashMap<>();
        Map<String, Table> tableMap = new LinkedHashMap<>();

        Set<String> tableNameSet = new HashSet<>();
        Set<String> tableAliasSet = new HashSet<>();
        for (Class<?> clazz : classes) {
            TableIgnore tableIgnore = clazz.getAnnotation(TableIgnore.class);
            if (QueryUtil.isNotNull(tableIgnore) && tableIgnore.value()) {
                continue;
            }

            TableInfo tableInfo = clazz.getAnnotation(TableInfo.class);
            String tableName, tableDesc, tableAlias;
            if (QueryUtil.isNotNull(tableInfo)) {
                tableAlias = QueryUtil.defaultIfBlank(tableInfo.alias(), clazz.getSimpleName());
                tableName = tableInfo.value();
                tableDesc = tableInfo.desc();
            } else {
                tableAlias = clazz.getSimpleName();
                tableName = QueryUtil.classToTableName(tablePrefix, tableAlias);
                tableDesc = "";
            }

            if (tableNameSet.contains(tableName)) {
                throw new RuntimeException("table(" + tableName + ") has renamed");
            }
            tableNameSet.add(tableName);
            if (tableAliasSet.contains(tableAlias)) {
                throw new RuntimeException("table alias(" + tableName + ") has renamed");
            }
            tableAliasSet.add(tableAlias);

            Set<String> columnNameSet = new HashSet<>();
            Set<String> columnAliasSet = new HashSet<>();
            Map<String, TableColumn> columnMap = new LinkedHashMap<>();
            String logicColumn = "", logicValue = "", logicDeleteValue = "";
            for (Field field : QueryUtil.getFields(clazz)) {
                ColumnIgnore columnIgnore = field.getAnnotation(ColumnIgnore.class);
                if (QueryUtil.isNotNull(columnIgnore) && columnIgnore.value()) {
                    continue;
                }

                ColumnInfo columnInfo = field.getAnnotation(ColumnInfo.class);
                Class<?> fieldType = field.getType();
                String columnName, columnDesc, columnAlias, fieldName = field.getName();
                boolean primary;
                Integer strLen;
                boolean notNull, hasDefault;
                if (QueryUtil.isNotNull(columnInfo)) {
                    columnName = QueryUtil.defaultIfBlank(columnInfo.value(), QueryUtil.fieldToColumnName(fieldName));

                    columnDesc = columnInfo.desc();
                    // 1. alias, 2. column-name, 3. field-name
                    columnAlias = QueryUtil.defaultIfBlank(QueryUtil.defaultIfBlank(columnInfo.alias(), columnName), fieldName);
                    primary = columnInfo.primary();
                    strLen = columnInfo.strLen();
                    notNull = columnInfo.notNull();
                    hasDefault = columnInfo.hasDefault();

                    RelationInfo ri = field.getAnnotation(RelationInfo.class);
                    if (QueryUtil.isNotNull(ri)) {
                        relationList.add(new TableColumnRelation(ri.masterTable(), ri.masterColumn(), ri.type(), tableName, columnName));
                    }
                } else {
                    columnDesc = "";
                    columnAlias = field.getName();
                    columnName = QueryUtil.fieldToColumnName(columnAlias);
                    primary = "id".equalsIgnoreCase(columnAlias);
                    strLen = null;
                    notNull = false;
                    hasDefault = false;
                }

                LogicDelete logicDelete = field.getAnnotation(LogicDelete.class);
                if (QueryUtil.isNotNull(logicDelete)) {
                    String columnLogicValue = logicDelete.value();
                    String columnLogicDeleteValue = logicDelete.deleteValue();
                    if (QueryUtil.isNotEmpty(columnLogicValue) && QueryUtil.isNotEmpty(columnLogicDeleteValue)) {
                        if (QueryUtil.isNotEmpty(logicColumn)) {
                            throw new RuntimeException("table(" + tableAlias + ") can only has one column with logic delete");
                        }
                        logicColumn = columnName;
                        logicValue = columnLogicValue;
                        logicDeleteValue = columnLogicDeleteValue;
                    }
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
        return new TableColumnInfo(aliasMap, tableClassMap, tableMap);
    }

    public static void checkAndSetRelation(List<TableColumnRelation> relationList, TableColumnInfo tcInfo) {
        if (QueryUtil.isNotEmpty(relationList)) {
            List<String> noTableList = new ArrayList<>();
            List<String> noColumnList = new ArrayList<>();
            List<String> typeErrorList = new ArrayList<>();

            Set<TableColumnRelation> set = new LinkedHashSet<>(relationList);
            for (TableColumnRelation relation : set) {
                String oneTable = relation.getOneTable();
                Table table = tcInfo.findTable(oneTable);

                String omTable = relation.getOneOrManyTable();
                Table oneOrManyTable = tcInfo.findTable(omTable);
                if (QueryUtil.isNull(table) || QueryUtil.isNull(oneOrManyTable)) {
                    if (QueryUtil.isNull(table)) {
                        noTableList.add(oneTable);
                    }
                    if (QueryUtil.isNull(oneOrManyTable)) {
                        noTableList.add(omTable);
                    }
                    continue;
                }

                String oneColumn = relation.getOneColumn();
                TableColumn tableColumn = tcInfo.findTableColumn(table, oneColumn);

                String omColumn = relation.getOneOrManyColumn();
                TableColumn oneOrManyColumn = tcInfo.findTableColumn(oneOrManyTable, omColumn);
                if (QueryUtil.isNull(tableColumn) || QueryUtil.isNull(oneOrManyColumn)) {
                    if (QueryUtil.isNull(tableColumn)) {
                        noColumnList.add(oneColumn);
                    }
                    if (QueryUtil.isNull(oneOrManyColumn)) {
                        noColumnList.add(omColumn);
                    }
                    continue;
                }

                Class<?> oneType = tableColumn.getFieldType();
                Class<?> omType = oneOrManyColumn.getFieldType();
                int oneLen = QueryUtil.toInt(tableColumn.getStrLen());
                int omLen = QueryUtil.toInt(oneOrManyColumn.getStrLen());
                if (oneType != omType || !Objects.equals(oneLen, omLen)) {
                    String one = oneTable + "." + oneColumn + "(" + oneType.getSimpleName() + (oneLen > 0 ? (":" + oneLen) : "") + ")";
                    String om = omTable + "." + omColumn + "(" + omType.getSimpleName() + (omLen > 0 ? (":" + omLen) : "") + ")";
                    typeErrorList.add(one + " : " + om);
                }
            }
            if (QueryUtil.isNotEmpty(noTableList)) {
                throw new RuntimeException("check relation: table(" + String.join(", ", noTableList) + ") has no defined");
            }
            if (QueryUtil.isNotEmpty(noColumnList)) {
                throw new RuntimeException("check relation: column(" + String.join(", ", noColumnList) + ") has no defined");
            }
            if (QueryUtil.isNotEmpty(typeErrorList)) {
                throw new RuntimeException("check relation: column data type or length { " + String.join(", ", typeErrorList) + " } error");
            }
            tcInfo.handleRelation(new ArrayList<>(set));
        }
    }


    public static TableColumnInfo infoWithDb(String tablePrefix,
                                             List<Map<String, Object>> tableList,
                                             List<Map<String, Object>> tableColumnList,
                                             String globalLogicColumn, String globalLogicValue,
                                             String globalLogicDeleteBooleanValue, String globalLogicDeleteIntValue,
                                             String globalLogicDeleteLongValue) {
        Map<String, String> aliasMap = new HashMap<>();
        Map<String, Table> tableMap = new LinkedHashMap<>();

        Map<String, List<Map<String, Object>>> tableColumnMap = new HashMap<>();
        tableColumnListToMap(tableColumnList, tableColumnMap);

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
                boolean notNull = !QueryUtil.toBool(QueryUtil.toStr(columnInfo.get("ine")));
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
        return new TableColumnInfo(aliasMap, new HashMap<>(), tableMap);
    }

    private static void tableColumnListToMap(List<Map<String, Object>> tableColumnList,
                                             Map<String, List<Map<String, Object>>> tableColumnMap) {
        if (QueryUtil.isNotEmpty(tableColumnList)) {
            for (Map<String, Object> tableColumn : tableColumnList) {
                String key = QueryUtil.toStr(tableColumn.get("tn"));
                tableColumnMap.computeIfAbsent(key, (k) -> new ArrayList<>()).add(tableColumn);
            }
        }
    }

    public static void generateModel(Set<String> tableSet, String targetPath, String packagePath,
                                     String modelSuffix, String tablePrefix,
                                     List<Map<String, Object>> tableList, List<Map<String, Object>> tableColumnList) {
        File packageDir = new File(targetPath.replace(".", "/"), packagePath.replace(".", "/"));
        if (!packageDir.exists()) {
            boolean flag = packageDir.mkdirs();
            if (!flag) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("directory({}) generate fail", packageDir);
                }
                return;
            }
        }

        Map<String, List<Map<String, Object>>> tableColumnMap = new HashMap<>();
        tableColumnListToMap(tableColumnList, tableColumnMap);

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
            String className = QueryUtil.tableNameToClass(tablePrefix, tableName) + QueryUtil.toStr(modelSuffix);
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
                    fieldSbd.append("    ").append(String.format("/** %s --> %s */\n", columnDesc, columnName));
                }

                List<String> columnInfoList = new ArrayList<>();
                if (!columnName.equals(fieldName)) {
                    columnInfoList.add(String.format("value = \"%s\"", columnName));
                }
                if (QueryUtil.isNotEmpty(columnDesc)) {
                    columnInfoList.add(String.format("desc = \"%s\"", columnDesc));
                }
                if (primary) {
                    columnInfoList.add("primary = true");
                }
                if (hasLen) {
                    columnInfoList.add("strLen = " + strLen);
                }
                if (notNull) {
                    columnInfoList.add("notNull = true");
                }
                if (hasDefault) {
                    columnInfoList.add("hasDefault = true");
                }
                if (QueryUtil.isNotEmpty(columnInfoList)) {
                    importSet.add("import " + ColumnInfo.class.getName() + ";");
                    fieldSbd.append("    ").append("@ColumnInfo(").append(String.join(", ", columnInfoList)).append(")\n");
                }

                String classType = fieldType.getName();
                if (!classType.startsWith("java.lang")) {
                    javaImportSet.add("import " + classType + ";");
                }
                fieldSbd.append("    ").append("private ").append(fieldType.getSimpleName()).append(" ").append(fieldName).append(";\n");
                fieldList.add(fieldSbd.toString());
            }

            List<String> tableInfoList = new ArrayList<>();
            if (!tableName.equals(className)) {
                tableInfoList.add(String.format("value = \"%s\"", tableName));
            }
            if (QueryUtil.isNotEmpty(tableDesc)) {
                tableInfoList.add(String.format("desc = \"%s\"", tableDesc));
            }

            importSet.add("import lombok.Data;");
            if (QueryUtil.isNotEmpty(tableInfoList)) {
                importSet.add("import " + TableInfo.class.getName() + ";");
            }

            sbd.append("package ").append(packagePath.replace("/", ".")).append(";\n\n");
            sbd.append(String.join("\n", importSet)).append("\n\n");
            sbd.append(String.join("\n", javaImportSet)).append("\n\n");
            sbd.append("@Data\n");
            if (QueryUtil.isNotEmpty(tableInfoList)) {
                sbd.append("@TableInfo(").append(String.join(" ,", tableInfoList)).append(")\n");
            }
            sbd.append("public class ").append(className).append(" {\n\n");
            sbd.append(String.join("\n", fieldList));
            sbd.append("}\n");

            File file = new File(packageDir, className + ".java");
            if (file.exists()) {
                boolean flag = file.delete();
                if (!flag) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("file({}) delete fail", file);
                    }
                }
            }
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
}
