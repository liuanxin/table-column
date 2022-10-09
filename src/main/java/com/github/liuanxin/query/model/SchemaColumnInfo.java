package com.github.liuanxin.query.model;

import com.github.liuanxin.query.constant.QueryConst;

import java.util.*;

public class SchemaColumnInfo {

    private final Map<String, String> aliasMap;
    private final Map<String, String> schemaClassMap;
    private final Map<String, Schema> schemaMap;

    private final Map<String, Map<String, SchemaColumnRelation>> childRelationMap;
    private final Map<String, Map<String, SchemaColumnRelation>> masterChildSchemaMap;

    public SchemaColumnInfo(Map<String, String> aliasMap, Map<String, String> schemaClassMap,
                            Map<String, Schema> schemaMap, List<SchemaColumnRelation> relationList) {
        this.aliasMap = aliasMap;
        this.schemaClassMap = schemaClassMap;
        this.schemaMap = schemaMap;

        Map<String, Map<String, SchemaColumnRelation>> childRelationMap = new HashMap<>();
        Map<String, Map<String, SchemaColumnRelation>> masterChildSchemaMap = new HashMap<>();
        if (relationList != null && !relationList.isEmpty()) {
            for (SchemaColumnRelation relation : relationList) {
                String masterSchema = relation.getOneSchema();
                String childSchema = relation.getOneOrManySchema();
                String childColumn = relation.getOneOrManyColumn();

                Map<String, SchemaColumnRelation> childRelation = childRelationMap.getOrDefault(childSchema, new HashMap<>());
                childRelation.put(childColumn, relation);
                childRelationMap.put(childSchema, childRelation);

                Map<String, SchemaColumnRelation> masterChildRelation = masterChildSchemaMap.getOrDefault(masterSchema, new HashMap<>());
                masterChildRelation.put(childSchema, relation);
                masterChildSchemaMap.put(masterSchema, masterChildRelation);
            }
        }
        this.childRelationMap = childRelationMap;
        this.masterChildSchemaMap = masterChildSchemaMap;
    }


    public Collection<Schema> allSchema() {
        return schemaMap.values();
    }

    public Schema findSchemaByClass(Class<?> clazz) {
        String schemaName = schemaClassMap.get(clazz.getName());
        return (schemaName == null || schemaName.isEmpty()) ? null : schemaMap.get(schemaName);
    }

    public Schema findSchema(String schemaName) {
        String schemaAlias = aliasMap.get(QueryConst.SCHEMA_PREFIX + schemaName);
        Schema schema = schemaMap.get(schemaAlias);
        return schema == null ? schemaMap.get(schemaName) : schema;
    }

    public SchemaColumn findSchemaColumn(Schema schema, String columnName) {
        Map<String, SchemaColumn> columnMap = schema.getColumnMap();
        String columnAlias = aliasMap.get(QueryConst.COLUMN_PREFIX + columnName);
        SchemaColumn schemaColumn = columnMap.get(columnAlias);
        return schemaColumn == null ? columnMap.get(columnName) : schemaColumn;
    }

    public SchemaColumn findSchemaColumn(String schemaName, String columnName) {
        Schema schema = findSchema(schemaName);
        return (schema == null) ? null : findSchemaColumn(schema, columnName);
    }

    public Map<String, SchemaColumnRelation> findRelationByMaster(String masterSchema) {
        if (masterSchema == null || masterSchema.isEmpty()) {
            return Collections.emptyMap();
        }

        String schemaAlias = aliasMap.get(QueryConst.SCHEMA_PREFIX + masterSchema);
        Map<String, SchemaColumnRelation> relationMap = masterChildSchemaMap.get(schemaAlias);
        return (relationMap == null || relationMap.isEmpty()) ? masterChildSchemaMap.get(masterSchema) : relationMap;
    }

    public SchemaColumnRelation findRelationByMasterChild(String masterSchema, String childSchema) {
        return findRelation(masterChildSchemaMap, masterSchema, childSchema);
    }

    private SchemaColumnRelation findRelation(Map<String, Map<String, SchemaColumnRelation>> schemaRelationMap,
                                              String schema, String childSchemaOrColumn) {
        if (schemaRelationMap == null || schemaRelationMap.isEmpty() || schema == null || schema.isEmpty()
                || childSchemaOrColumn == null || childSchemaOrColumn.isEmpty()) {
            return null;
        }

        String schemaAlias = aliasMap.get(QueryConst.SCHEMA_PREFIX + schema);
        Map<String, SchemaColumnRelation> relationMap = schemaRelationMap.get(schemaAlias);
        Map<String, SchemaColumnRelation> useRelationMap =
                (relationMap == null || relationMap.isEmpty()) ? schemaRelationMap.get(schema) : relationMap;
        if (useRelationMap == null || useRelationMap.isEmpty()) {
            return null;
        }

        String columnAlias = aliasMap.get(QueryConst.COLUMN_PREFIX + childSchemaOrColumn);
        SchemaColumnRelation relation = useRelationMap.get(columnAlias);
        return (relation == null) ? useRelationMap.get(childSchemaOrColumn) : relation;
    }

    public SchemaColumnRelation findRelationByChild(String childSchema, String childColumn) {
        return findRelation(childRelationMap, childSchema, childColumn);
    }
}
