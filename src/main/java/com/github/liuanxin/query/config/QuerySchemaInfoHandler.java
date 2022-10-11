package com.github.liuanxin.query.config;

import com.github.liuanxin.query.constant.QueryConst;
import com.github.liuanxin.query.enums.ResultType;
import com.github.liuanxin.query.enums.SchemaRelationType;
import com.github.liuanxin.query.model.*;
import com.github.liuanxin.query.util.QueryJsonUtil;
import com.github.liuanxin.query.util.QueryScanUtil;
import com.github.liuanxin.query.util.QuerySqlUtil;
import com.github.liuanxin.query.util.QueryUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.*;

@Component
public class QuerySchemaInfoHandler implements InitializingBean {

    @Value("${query.scan-packages:}")
    private String scanPackages;

    @Value("${query.deep-max-page-size:10000}")
    private int deepMaxPageSize;

    private SchemaColumnInfo scInfo;

    private final JdbcTemplate jdbcTemplate;

    public QuerySchemaInfoHandler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterPropertiesSet() {
        scInfo = (scanPackages == null || scanPackages.isEmpty()) ? initWithDb() : QueryScanUtil.scanSchema(scanPackages);
    }

    private SchemaColumnInfo initWithDb() {
        Map<String, String> aliasMap = new HashMap<>();
        Map<String, Schema> schemaMap = new LinkedHashMap<>();
        List<SchemaColumnRelation> relationList = new ArrayList<>();

        String dbName = jdbcTemplate.queryForObject(QueryConst.DB_SQL, String.class);
        // schema_name, schema_comment
        List<Map<String, Object>> schemaList = jdbcTemplate.queryForList(QueryConst.SCHEMA_SQL, dbName);
        // schema_name, column_name, column_type, column_comment, has_pri, varchar_length
        List<Map<String, Object>> schemaColumnList = jdbcTemplate.queryForList(QueryConst.COLUMN_SQL, dbName);
        // schema_name, column_name, relation_schema_name, relation_column_name (relation : one or many)
        List<Map<String, Object>> relationColumnList = jdbcTemplate.queryForList(QueryConst.RELATION_SQL, dbName);
        // schema_name, column_name, has_single_unique
        List<Map<String, Object>> indexList = jdbcTemplate.queryForList(QueryConst.INDEX_SQL, dbName);

        Map<String, List<Map<String, Object>>> schemaColumnMap = new HashMap<>();
        if (!schemaColumnList.isEmpty()) {
            for (Map<String, Object> schemaColumn : schemaColumnList) {
                String key = QueryUtil.toStr(schemaColumn.get("tn"));
                schemaColumnMap.computeIfAbsent(key, (k1) -> new ArrayList<>()).add(schemaColumn);
            }
        }
        Map<String, Map<String, Map<String, Object>>> relationColumnMap = new HashMap<>();
        if (!relationColumnList.isEmpty()) {
            for (Map<String, Object> relationColumn : relationColumnList) {
                String schemaName = QueryUtil.toStr(relationColumn.get("tn"));
                Map<String, Map<String, Object>> columnMap = relationColumnMap.getOrDefault(schemaName, new HashMap<>());
                columnMap.put(QueryUtil.toStr(relationColumn.get("cn")), relationColumn);
                relationColumnMap.put(schemaName, columnMap);
            }
        }
        Map<String, Set<String>> columnUniqueMap = new HashMap<>();
        if (!indexList.isEmpty()) {
            for (Map<String, Object> index : indexList) {
                String schemaName = QueryUtil.toStr(index.get("tn"));
                Set<String> uniqueColumnSet = columnUniqueMap.getOrDefault(schemaName, new HashSet<>());
                uniqueColumnSet.add(QueryUtil.toStr(index.get("cn")));
                columnUniqueMap.put(schemaName, uniqueColumnSet);
            }
        }

        for (Map<String, Object> schemaInfo : schemaList) {
            String schemaName = QueryUtil.toStr(schemaInfo.get("tn"));
            String schemaAlias = QueryUtil.schemaNameToAlias(schemaName);
            String schemaDesc = QueryUtil.toStr(schemaInfo.get("tc"));
            Map<String, SchemaColumn> columnMap = new LinkedHashMap<>();

            List<Map<String, Object>> columnList = schemaColumnMap.get(schemaName);
            for (Map<String, Object> columnInfo : columnList) {
                Class<?> fieldType = QueryUtil.mappingClass(QueryUtil.toStr(columnInfo.get("ct")));
                String columnName = QueryUtil.toStr(columnInfo.get("cn"));
                String columnAlias = QueryUtil.columnNameToAlias(columnName);
                String columnDesc = QueryUtil.toStr(columnInfo.get("cc"));
                boolean primary = "PRI".equalsIgnoreCase(QueryUtil.toStr(columnInfo.get("ck")));
                Integer strLen = QueryUtil.toInteger(QueryUtil.toStr(columnInfo.get("cml")));

                aliasMap.put(QueryConst.COLUMN_PREFIX + columnName, columnAlias);
                columnMap.put(columnAlias, new SchemaColumn(columnName, columnDesc, columnAlias, primary,
                        ((strLen == null || strLen <= 0) ? null : strLen), fieldType, columnAlias));
            }
            aliasMap.put(QueryConst.SCHEMA_PREFIX + schemaName, schemaAlias);
            schemaMap.put(schemaAlias, new Schema(schemaName, schemaDesc, schemaAlias, columnMap));
        }

        if (!relationColumnMap.isEmpty()) {
            for (Map.Entry<String, Map<String, Map<String, Object>>> entry : relationColumnMap.entrySet()) {
                String relationSchema = entry.getKey();
                Set<String> uniqueColumnSet = columnUniqueMap.get(relationSchema);
                for (Map.Entry<String, Map<String, Object>> columnEntry : entry.getValue().entrySet()) {
                    String relationColumn = columnEntry.getKey();
                    SchemaRelationType type = uniqueColumnSet.contains(relationColumn)
                            ? SchemaRelationType.ONE_TO_ONE : SchemaRelationType.ONE_TO_MANY;

                    Map<String, Object> relationInfoMap = columnEntry.getValue();
                    String oneSchema = QueryUtil.toStr(relationInfoMap.get("ftn"));
                    String oneColumn = QueryUtil.toStr(relationInfoMap.get("fcn"));

                    relationList.add(new SchemaColumnRelation(oneSchema, oneColumn, type, relationSchema, relationColumn));
                }
            }
        }
        return new SchemaColumnInfo(aliasMap, new HashMap<>(), schemaMap, relationList);
    }


    public List<QueryInfo> info(String schemas) {
        Set<String> schemaSet = new LinkedHashSet<>();
        if (schemas != null && !schemas.isEmpty()) {
            for (String te : schemas.split(",")) {
                String trim = te.trim();
                if (!trim.isEmpty()) {
                    schemaSet.add(trim.toLowerCase());
                }
            }
        }
        List<QueryInfo> queryList = new ArrayList<>();
        for (Schema schema : scInfo.allSchema()) {
            if (schemaSet.isEmpty() || schemaSet.contains(schema.getAlias().toLowerCase())) {
                List<QueryInfo.QueryColumn> columnList = new ArrayList<>();
                for (SchemaColumn sc : schema.getColumnMap().values()) {
                    String type = sc.getColumnType().getSimpleName();
                    Integer length = sc.getStrLen();
                    SchemaColumnRelation relation = scInfo.findRelationByChild(schema.getName(), sc.getName());
                    String schemaColumn = (relation == null) ? null : (relation.getOneSchema() + "." + relation.getOneColumn());
                    columnList.add(new QueryInfo.QueryColumn(sc.getAlias(), sc.getDesc(), type, length, schemaColumn));
                }
                queryList.add(new QueryInfo(schema.getAlias(), schema.getDesc(), columnList));
            }
        }
        return queryList;
    }


    public <T> int insert(T obj) {
        return insert(obj, false);
    }

    public <T> int insert(T obj, boolean generateNullField) {
        if (obj == null) {
            return 0;
        }

        Schema schema = scInfo.findSchemaByClass(obj.getClass());
        if (schema == null) {
            return 0;
        }

        List<Object> params = new ArrayList<>();
        String insertSql = schema.generateInsert(obj, generateNullField, params);
        if (insertSql != null && !insertSql.isEmpty() && !params.isEmpty()) {
            return jdbcTemplate.update(insertSql, params.toArray());
        } else {
            return 0;
        }
    }

    public <T> int insertBatch(List<T> list) {
        return insertBatch(list, 500, false);
    }

    public <T> int insertBatch(List<T> list, int singleCount) {
        return insertBatch(list, singleCount, false);
    }

    public <T> int insertBatch(List<T> list, int singleCount, boolean generateNullField) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        Schema schema = scInfo.findSchemaByClass(list.get(0).getClass());
        if (schema == null) {
            return 0;
        }

        int flag = 0;
        List<List<T>> splitList = QueryUtil.split(list, singleCount);
        for (List<T> lt : splitList) {
            List<Object> params = new ArrayList<>();
            String insertSql = schema.generateBatchInsert(lt, generateNullField, params);
            if (insertSql != null && !insertSql.isEmpty()) {
                flag += jdbcTemplate.update(insertSql, params.toArray());
            }
        }
        return flag;
    }


    public <T> int deleteById(Class<T> clazz, Serializable id) {
        if (clazz == null) {
            return 0;
        }

        Schema schema = scInfo.findSchemaByClass(clazz);
        if (schema == null) {
            return 0;
        }
        return delete(clazz, SingleSchemaWhere.buildId(schema.idWhere(false), id));
    }

    public <T> int deleteByIds(Class<T> clazz, List<Serializable> ids) {
        if (clazz == null) {
            return 0;
        }

        Schema schema = scInfo.findSchemaByClass(clazz);
        if (schema == null) {
            return 0;
        }
        return delete(clazz, SingleSchemaWhere.buildIds(schema.idWhere(false), ids));
    }

    public <T> int delete(Class<T> clazz, SingleSchemaWhere query) {
        if (clazz == null || query == null) {
            return 0;
        }

        Schema schema = scInfo.findSchemaByClass(clazz);
        if (schema == null) {
            return 0;
        }

        List<Object> params = new ArrayList<>();
        String deleteSql = schema.generateDelete(query, scInfo, params);
        if (deleteSql != null && !deleteSql.isEmpty()) {
            return jdbcTemplate.update(deleteSql, params.toArray());
        } else {
            return 0;
        }
    }


    public <T> int updateById(T updateObj, Serializable id) {
        if (updateObj == null) {
            return 0;
        }

        Schema schema = scInfo.findSchemaByClass(updateObj.getClass());
        if (schema == null) {
            return 0;
        }
        return update(updateObj, SingleSchemaWhere.buildId(schema.idWhere(false), id));
    }

    public <T> int updateByIds(T updateObj, List<Serializable> ids) {
        if (updateObj == null) {
            return 0;
        }

        Schema schema = scInfo.findSchemaByClass(updateObj.getClass());
        if (schema == null) {
            return 0;
        }
        return update(updateObj, SingleSchemaWhere.buildIds(schema.idWhere(false), ids));
    }

    public <T> int update(T updateObj, SingleSchemaWhere query) {
        return update(updateObj, query, false);
    }

    public <T> int update(T updateObj, SingleSchemaWhere query, boolean generateNullField) {
        if (updateObj == null || query == null) {
            return 0;
        }

        Schema schema = scInfo.findSchemaByClass(updateObj.getClass());
        if (schema == null) {
            return 0;
        }

        List<Object> params = new ArrayList<>();
        String updateSql = schema.generateUpdate(updateObj, generateNullField, query, scInfo, params);
        if (updateSql != null && !updateSql.isEmpty()) {
            return jdbcTemplate.update(updateSql, params.toArray());
        } else {
            return 0;
        }
    }


    public Object query(RequestInfo req) {
        req.checkSchema(scInfo);
        Set<String> paramSchemaSet = req.checkParam(scInfo);

        Set<String> allResultSchemaSet = new LinkedHashSet<>();
        Set<String> resultFuncSchemaSet = req.checkResult(scInfo, allResultSchemaSet);

        List<SchemaJoinRelation> allRelationList = req.allRelationList(scInfo);
        Set<String> allSchemaSet = calcSchemaSet(allRelationList);
        req.checkAllSchema(scInfo, allSchemaSet, paramSchemaSet, allResultSchemaSet);

        String mainSchema = req.getSchema();
        ReqParam param = req.getParam();
        ReqResult result = req.getResult();

        String allFromSql = QuerySqlUtil.toFromSql(scInfo, mainSchema, allRelationList);
        List<Object> params = new ArrayList<>();
        if (param.needQueryPage()) {
            if (param.needQueryCount()) {
                List<SchemaJoinRelation> paramRelationList = req.paramRelationList(scInfo, paramSchemaSet, resultFuncSchemaSet);
                Set<String> querySchemaSet = calcSchemaSet(paramRelationList);
                boolean queryHasMany = calcQueryHasMany(paramRelationList);

                String firstFromSql = QuerySqlUtil.toFromSql(scInfo, mainSchema, paramRelationList);
                String whereSql = QuerySqlUtil.toWhereSql(scInfo, mainSchema, !querySchemaSet.isEmpty(), param, params);
                return queryPage(firstFromSql, allFromSql, whereSql, mainSchema, param, result, queryHasMany, querySchemaSet, allSchemaSet, params);
            } else {
                String whereSql = QuerySqlUtil.toWhereSql(scInfo, mainSchema, !allSchemaSet.isEmpty(), param, params);
                return queryList(allFromSql + whereSql, mainSchema, param, result, allSchemaSet, params);
            }
        } else {
            String whereSql = QuerySqlUtil.toWhereSql(scInfo, mainSchema, !allSchemaSet.isEmpty(), param, params);
            if (req.getType() == ResultType.OBJ) {
                return queryObj(allFromSql + whereSql, mainSchema, param, result, allSchemaSet, params);
            } else {
                return queryListNoLimit(allFromSql + whereSql, mainSchema, param, result, allSchemaSet, params);
            }
        }
    }

    private Set<String> calcSchemaSet(List<SchemaJoinRelation> relationList) {
        Set<String> schemaSet = new HashSet<>();
        for (SchemaJoinRelation joinRelation : relationList) {
            schemaSet.add(joinRelation.getMasterSchema().getName());
            schemaSet.add(joinRelation.getChildSchema().getName());
        }
        return schemaSet;
    }
    private boolean calcQueryHasMany(List<SchemaJoinRelation> paramRelationList) {
        for (SchemaJoinRelation joinRelation : paramRelationList) {
            String masterSchemaName = joinRelation.getMasterSchema().getName();
            String childSchemaName = joinRelation.getChildSchema().getName();
            SchemaColumnRelation relation = scInfo.findRelationByMasterChild(masterSchemaName, childSchemaName);
            if (relation != null && relation.getType().hasMany()) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> queryPage(String firstFromSql, String allFromSql, String whereSql, String mainSchema,
                                          ReqParam param, ReqResult result, boolean queryHasMany,
                                          Set<String> querySchemaSet, Set<String> allSchemaSet, List<Object> params) {
        String fromAndWhere = firstFromSql + whereSql;
        long count;
        List<Map<String, Object>> pageList;
        if (result.needGroup()) {
            // SELECT COUNT(*) FROM ( SELECT ... FROM ... WHERE .?. GROUP BY ... HAVING ... ) tmp    (only where's schema)
            String selectCountGroupSql = QuerySqlUtil.toSelectGroupSql(scInfo, fromAndWhere, mainSchema, result, querySchemaSet, params);
            count = queryCount(QuerySqlUtil.toCountGroupSql(selectCountGroupSql), params);
            if (param.needQueryCurrentPage(count)) {
                String fromAndWhereList = allFromSql + whereSql;
                // SELECT ... FROM ... WHERE .?. GROUP BY ... HAVING ... LIMIT ...    (all where's schema)
                String selectListGroupSql = QuerySqlUtil.toSelectGroupSql(scInfo, fromAndWhereList, mainSchema, result, allSchemaSet, params);
                pageList = queryPageListWithGroup(selectListGroupSql, mainSchema, allSchemaSet, param, result, params);
            } else {
                pageList = Collections.emptyList();
            }
        } else {
            boolean needAlias = !querySchemaSet.isEmpty();
            // SELECT COUNT(DISTINCT id) FROM ... WHERE .?..   (only where's schema)
            String countSql = QuerySqlUtil.toCountWithoutGroupSql(scInfo, mainSchema, needAlias, queryHasMany, fromAndWhere);
            count = queryCount(countSql, params);
            if (param.needQueryCurrentPage(count)) {
                pageList = queryPageListWithoutGroup(firstFromSql, allFromSql, whereSql, mainSchema, param,
                        result, needAlias, allSchemaSet, params);
            } else {
                pageList = Collections.emptyList();
            }
        }
        Map<String, Object> pageInfo = new LinkedHashMap<>();
        pageInfo.put("count", count);
        pageInfo.put("list", pageList);
        return pageInfo;
    }

    private long queryCount(String countSql, List<Object> params) {
        Long count = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        return count == null ? 0L : count;
    }

    private List<Map<String, Object>> queryPageListWithoutGroup(String firstFromSql, String allFromSql,
                                                                String whereSql, String mainSchema, ReqParam param,
                                                                ReqResult result, boolean needAlias,
                                                                Set<String> allSchemaSet, List<Object> params) {
        String fromAndWhere = firstFromSql + whereSql;
        String sql;
        // deep paging(need offset a lot of result), use 「where + order + limit」 to query id, then use id to query specific columns
        if (param.hasDeepPage(deepMaxPageSize)) {
            // SELECT id FROM ... WHERE .?. ORDER BY ... LIMIT ...   (only where's schema)
            String idPageSql = QuerySqlUtil.toIdPageSql(scInfo, fromAndWhere, mainSchema, needAlias, param, params);
            List<Map<String, Object>> idList = jdbcTemplate.queryForList(idPageSql, params.toArray());

            // SELECT ... FROM .?. WHERE id IN (...)    (all where's schema)
            params.clear();
            sql = QuerySqlUtil.toSelectWithIdSql(scInfo, mainSchema, allFromSql, result, idList, allSchemaSet, params);
        } else {
            // SELECT ... FROM ... WHERE ... ORDER BY ... limit ...
            sql = QuerySqlUtil.toPageWithoutGroupSql(scInfo, fromAndWhere, mainSchema, param, result, allSchemaSet, params);
        }
        return assemblyResult(sql, params, mainSchema, allSchemaSet, result);
    }

    private List<Map<String, Object>> queryPageListWithGroup(String selectGroupSql, String mainSchema, Set<String> allSchemaSet,
                                                             ReqParam param, ReqResult result, List<Object> params) {
        String sql = selectGroupSql + param.generatePageSql(params);
        return assemblyResult(sql, params, mainSchema, allSchemaSet, result);
    }

    private List<Map<String, Object>> queryList(String fromAndWhere, String mainSchema, ReqParam param,
                                                ReqResult result, Set<String> allSchemaSet, List<Object> params) {
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(scInfo, fromAndWhere, mainSchema, result, allSchemaSet, params);
        String orderSql = param.generateOrderSql(mainSchema, !allSchemaSet.isEmpty(), scInfo);
        String sql = selectGroupSql + orderSql + param.generatePageSql(params);
        return assemblyResult(sql, params, mainSchema, allSchemaSet, result);
    }

    private List<Map<String, Object>> queryListNoLimit(String fromAndWhere, String mainSchema, ReqParam param,
                                                       ReqResult result, Set<String> allSchemaSet, List<Object> params) {
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(scInfo, fromAndWhere, mainSchema, result, allSchemaSet, params);
        String orderSql = param.generateOrderSql(mainSchema, !allSchemaSet.isEmpty(), scInfo);
        String sql = selectGroupSql + orderSql;
        return assemblyResult(sql, params, mainSchema, allSchemaSet, result);
    }

    private Map<String, Object> queryObj(String fromAndWhere, String mainSchema, ReqParam param, ReqResult result,
                                         Set<String> allSchemaSet, List<Object> params) {
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(scInfo, fromAndWhere, mainSchema, result, allSchemaSet, params);
        String orderSql = param.generateOrderSql(mainSchema, !allSchemaSet.isEmpty(), scInfo);
        String sql = selectGroupSql + orderSql + param.generateArrToObjSql(params);
        Map<String, Object> obj = QueryUtil.first(assemblyResult(sql, params, mainSchema, allSchemaSet, result));
        return (obj == null) ? Collections.emptyMap() : obj;
    }

    private List<Map<String, Object>> assemblyResult(String mainSql, List<Object> params, String mainSchema,
                                                     Set<String> allSchemaSet, ReqResult result) {
        List<Map<String, Object>> mapList = jdbcTemplate.queryForList(mainSql, params.toArray());
        if (!mapList.isEmpty()) {
            boolean needAlias = !allSchemaSet.isEmpty();
            Schema schema = scInfo.findSchema(mainSchema);
            List<String> idKeyList = schema.getIdKey();

            Set<String> selectColumnSet = result.selectColumn(mainSchema, scInfo, allSchemaSet);
            List<String> removeColumnList = new ArrayList<>();
            for (String ic : result.innerColumn(mainSchema, scInfo, needAlias)) {
                if (!selectColumnSet.contains(ic)) {
                    removeColumnList.add(ic);
                }
            }

            Map<String, ReqResult> innerMap = result.innerResult();
            Map<String, List<Map<String, Object>>> innerColumnMap = queryInnerData(schema, result);
            for (Map<String, Object> data : mapList) {
                fillInnerData(data, idKeyList, innerMap, innerColumnMap);
                removeColumnList.forEach(data::remove);
                result.handleDateType(data, mainSchema, scInfo);
            }
        }
        return mapList;
    }

    private Map<String, List<Map<String, Object>>> queryInnerData(Schema mainSchema, ReqResult result) {
        // todo
        Map<String, List<Map<String, Object>>> innerMap = new HashMap<>();
        for (Object obj : result.getColumns()) {
            if (obj != null) {
                if (!(obj instanceof String) && !(obj instanceof List<?>)) {
                    Map<String, ReqResult> inner = QueryJsonUtil.convertInnerResult(obj);
                    if (inner != null) {
                        for (Map.Entry<String, ReqResult> entry : inner.entrySet()) {
                            String innerName = entry.getKey();
                            innerMap.put(innerName + "-id", queryInnerData(entry.getValue()));
                        }
                    }
                }
            }
        }
        return innerMap;
    }
    private List<Map<String, Object>> queryInnerData(ReqResult result) {
        return null;
    }

    private void fillInnerData(Map<String, Object> data, List<String> idKeyList, Map<String, ReqResult> innerMap,
                               Map<String, List<Map<String, Object>>> innerColumnMap) {
        // todo
        for (Map.Entry<String, ReqResult> entry : innerMap.entrySet()) {
            String innerColumn = entry.getKey();
            data.put(innerColumn, innerColumnMap.get(innerColumn));
        }
    }
}
