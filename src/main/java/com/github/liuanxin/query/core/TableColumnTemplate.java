package com.github.liuanxin.query.core;

import com.github.liuanxin.query.constant.QueryConst;
import com.github.liuanxin.query.enums.ResultType;
import com.github.liuanxin.query.model.*;
import com.github.liuanxin.query.util.QueryInfoUtil;
import com.github.liuanxin.query.util.QuerySqlUtil;
import com.github.liuanxin.query.util.QueryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.*;

@Component
public class TableColumnTemplate implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(TableColumnTemplate.class);

    @Value("${query.table-prefix:}")
    private String tablePrefix;

    @Value("${query.scan-packages:}")
    private String scanPackages;

    @Value("${query.deep-max-page-size:10000}")
    private int deepMaxPageSize;

    @Value("${query.max-list-count:1000}")
    private int maxListCount;

    @Value("${query.one-to-one-has-many-exception:false}")
    private boolean oneToOneHasManyException;

    private TableColumnInfo tcInfo;

    private final JdbcTemplate jdbcTemplate;
    public TableColumnTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterPropertiesSet() {
        if (QueryUtil.isNotEmpty(scanPackages)) {
            tcInfo = QueryInfoUtil.infoWithScan(tablePrefix, scanPackages);
        } else {
            String dbName = jdbcTemplate.queryForObject(QueryConst.DB_SQL, String.class);
            // table_name, table_comment
            List<Map<String, Object>> tableList = jdbcTemplate.queryForList(QueryConst.TABLE_SQL, dbName);
            // table_name, column_name, column_type, column_comment, has_pri, varchar_length
            List<Map<String, Object>> tableColumnList = jdbcTemplate.queryForList(QueryConst.COLUMN_SQL, dbName);
            // table_name, column_name, relation_table_name, relation_column_name (relation : one or many)
            List<Map<String, Object>> relationColumnList = jdbcTemplate.queryForList(QueryConst.RELATION_SQL, dbName);
            // table_name, column_name, has_single_unique
            List<Map<String, Object>> indexList = jdbcTemplate.queryForList(QueryConst.INDEX_SQL, dbName);
            tcInfo = QueryInfoUtil.infoWithDb(tableList, tableColumnList, relationColumnList, indexList);
        }
    }


    public List<QueryInfo> info(String tables) {
        Set<String> tableSet = new LinkedHashSet<>();
        if (QueryUtil.isNotEmpty(tables)) {
            for (String te : tables.split(",")) {
                String trim = te.trim();
                if (!trim.isEmpty()) {
                    tableSet.add(trim.toLowerCase());
                }
            }
        }
        List<QueryInfo> queryList = new ArrayList<>();
        for (Table table : tcInfo.allTable()) {
            if (tableSet.isEmpty() || tableSet.contains(table.getAlias().toLowerCase())) {
                List<QueryInfo.QueryColumn> columnList = new ArrayList<>();
                for (TableColumn sc : table.getColumnMap().values()) {
                    String type = sc.getColumnType().getSimpleName();
                    Integer length = sc.getStrLen();
                    TableColumnRelation relation = tcInfo.findRelationByChild(table.getName(), sc.getName());
                    String tableColumn;
                    if (QueryUtil.isNull(relation)) {
                        tableColumn = null;
                    } else {
                        Table tb = tcInfo.findTable(relation.getOneTable());
                        TableColumn tc = tb.getColumnMap().get(relation.getOneColumn());
                        tableColumn = tb.getAlias() + "." + tc.getAlias();
                    }
                    columnList.add(new QueryInfo.QueryColumn(sc.getAlias(), sc.getDesc(), type, length, tableColumn));
                }
                queryList.add(new QueryInfo(table.getAlias(), table.getDesc(), columnList));
            }
        }
        return queryList;
    }


    public int insert(String table, Map<String, Object> data) {
        return insert(table, data, false);
    }

    public int insert(String table, Map<String, Object> data, boolean generateNullField) {
        if (QueryUtil.isEmpty(table) || QueryUtil.isNotEmpty(data)) {
            return 0;
        }

        Table tableInfo = tcInfo.findTable(table.trim());
        if (QueryUtil.isNull(tableInfo)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("insert: has no table({}) defined", table);
            }
            return 0;
        }

        List<Object> params = new ArrayList<>();
        String insertSql = tableInfo.generateInsertMap(data, generateNullField, params);
        if (QueryUtil.isEmpty(insertSql) || QueryUtil.isEmpty(params)) {
            return 0;
        }
        return jdbcTemplate.update(insertSql, params.toArray());
    }

    public int insertBatch(String table, List<Map<String, Object>> list) {
        return insertBatch(table, list, 500);
    }

    public int insertBatch(String table, List<Map<String, Object>> list, int singleCount) {
        return insertBatch(table, list, singleCount, false);
    }

    public int insertBatch(String table, List<Map<String, Object>> list, int singleCount, boolean generateNullField) {
        if (QueryUtil.isEmpty(table) || QueryUtil.isEmpty(list)) {
            return 0;
        }

        Table tableInfo = tcInfo.findTable(table);
        if (QueryUtil.isNull(tableInfo)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("insert: has no table({}) defined", table);
            }
            return 0;
        }

        int flag = 0;
        for (List<Map<String, Object>> lt : QueryUtil.split(list, singleCount)) {
            List<Object> params = new ArrayList<>();
            String batchInsertSql = tableInfo.generateBatchInsertMap(lt, generateNullField, params);
            if (QueryUtil.isNotEmpty(batchInsertSql)) {
                flag += jdbcTemplate.update(batchInsertSql, params.toArray());
            }
        }
        return flag;
    }

    public <T> int insert(T obj) {
        return insert(obj, false);
    }

    public <T> int insert(T obj, boolean generateNullField) {
        if (QueryUtil.isNull(obj)) {
            return 0;
        }

        Class<?> clazz = obj.getClass();
        Table table = tcInfo.findTableByClass(clazz);
        if (QueryUtil.isNull(table)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("insert: class({}) has no table defined", clazz.getName());
            }
            return 0;
        }

        List<Object> params = new ArrayList<>();
        String insertSql = table.generateInsert(obj, generateNullField, params);
        if (QueryUtil.isEmpty(insertSql) || QueryUtil.isEmpty(params)) {
            return 0;
        }
        return jdbcTemplate.update(insertSql, params.toArray());
    }

    public <T> int insertBatch(List<T> list) {
        return insertBatch(list, 500);
    }

    public <T> int insertBatch(List<T> list, int singleCount) {
        return insertBatch(list, singleCount, false);
    }

    public <T> int insertBatch(List<T> list, int singleCount, boolean generateNullField) {
        if (QueryUtil.isEmpty(list)) {
            return 0;
        }

        Class<?> clazz = list.get(0).getClass();
        Table table = tcInfo.findTableByClass(clazz);
        if (QueryUtil.isNull(table)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("insert: class({}) has no table defined", clazz.getName());
            }
            return 0;
        }

        int flag = 0;
        List<List<T>> splitList = QueryUtil.split(list, singleCount);
        for (List<T> lt : splitList) {
            List<Object> params = new ArrayList<>();
            String batchInsertSql = table.generateBatchInsert(lt, generateNullField, params);
            if (QueryUtil.isNotEmpty(batchInsertSql)) {
                flag += jdbcTemplate.update(batchInsertSql, params.toArray());
            }
        }
        return flag;
    }


    public int deleteById(String table, Serializable id) {
        if (QueryUtil.isEmpty(table) || QueryUtil.isIllegalId(id)) {
            return 0;
        }

        Table tableInfo = tcInfo.findTable(table.trim());
        if (QueryUtil.isNull(tableInfo)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("delete: has no table({}) defined", table);
            }
            return 0;
        }
        return delete(table, SingleTableWhere.buildId(tableInfo.idWhere(false), id));
    }

    public int deleteByIds(String table, List<Serializable> ids) {
        if (QueryUtil.isEmpty(table) || QueryUtil.isIllegalIdList(ids)) {
            return 0;
        }

        Table tableInfo = tcInfo.findTable(table.trim());
        if (QueryUtil.isNull(tableInfo)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("delete: has no table({}) defined", table);
            }
            return 0;
        }
        return delete(table, SingleTableWhere.buildIds(tableInfo.idWhere(false), ids));
    }

    public int delete(String table, SingleTableWhere query) {
        if (QueryUtil.isEmpty(table) || QueryUtil.isNull(query)) {
            return 0;
        }

        Table tableInfo = tcInfo.findTable(table.trim());
        if (QueryUtil.isNull(tableInfo)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("delete: has no table({}) defined", table);
            }
            return 0;
        }
        return doDelete(query, tableInfo);
    }

    public <T> int deleteById(Class<T> clazz, Serializable id) {
        if (QueryUtil.isNull(clazz) || QueryUtil.isIllegalId(id)) {
            return 0;
        }

        Table table = tcInfo.findTableByClass(clazz);
        if (QueryUtil.isNull(table)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("delete: class({}) has no table defined", clazz.getName());
            }
            return 0;
        }
        return delete(clazz, SingleTableWhere.buildId(table.idWhere(false), id));
    }

    public <T> int deleteByIds(Class<T> clazz, List<Serializable> ids) {
        if (QueryUtil.isNull(clazz) || QueryUtil.isIllegalIdList(ids)) {
            return 0;
        }

        Table table = tcInfo.findTableByClass(clazz);
        if (QueryUtil.isNull(table)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("delete: class({}) has no table defined", clazz.getName());
            }
            return 0;
        }
        return delete(clazz, SingleTableWhere.buildIds(table.idWhere(false), ids));
    }

    public <T> int delete(Class<T> clazz, SingleTableWhere query) {
        if (QueryUtil.isNull(clazz) || QueryUtil.isNull(query)) {
            return 0;
        }

        Table table = tcInfo.findTableByClass(clazz);
        if (QueryUtil.isNull(table)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("delete: class({}) has no table defined", clazz.getName());
            }
            return 0;
        }
        return doDelete(query, table);
    }

    private int doDelete(SingleTableWhere query, Table table) {
        List<Object> params = new ArrayList<>();
        String deleteSql = table.generateDelete(query, tcInfo, params);
        if (QueryUtil.isEmpty(deleteSql)) {
            return 0;
        }
        return jdbcTemplate.update(deleteSql, params.toArray());
    }


    public int updateById(String table, Map<String, Object> updateObj, Serializable id) {
        if (QueryUtil.isEmpty(table) || QueryUtil.isEmpty(updateObj) || QueryUtil.isIllegalId(id)) {
            return 0;
        }

        Table tableInfo = tcInfo.findTable(table.trim());
        if (QueryUtil.isNull(tableInfo)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("update: has no table({}) defined", table);
            }
            return 0;
        }
        return update(table, updateObj, SingleTableWhere.buildId(tableInfo.idWhere(false), id));
    }

    public int updateByIds(String table, Map<String, Object> updateObj, List<Serializable> ids) {
        if (QueryUtil.isEmpty(table) || QueryUtil.isEmpty(updateObj) || QueryUtil.isIllegalIdList(ids)) {
            return 0;
        }

        Table tableInfo = tcInfo.findTable(table.trim());
        if (QueryUtil.isNull(tableInfo)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("update: has no table({}) defined", table);
            }
            return 0;
        }
        return update(table, updateObj, SingleTableWhere.buildIds(tableInfo.idWhere(false), ids));
    }

    public int update(String table, Map<String, Object> updateObj, SingleTableWhere query) {
        return update(table, updateObj, query, false);
    }

    public int update(String table, Map<String, Object> updateObj, SingleTableWhere query, boolean generateNullField) {
        if (QueryUtil.isEmpty(table) || QueryUtil.isEmpty(updateObj) || QueryUtil.isNull(query)) {
            return 0;
        }

        Table tableInfo = tcInfo.findTable(table.trim());
        if (QueryUtil.isNull(tableInfo)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("update: has no table({}) defined", table);
            }
            return 0;
        }

        List<Object> params = new ArrayList<>();
        String updateSql = tableInfo.generateUpdateMap(updateObj, generateNullField, query, tcInfo, params);
        if (QueryUtil.isEmpty(updateSql) || QueryUtil.isEmpty(params)) {
            return 0;
        }
        return jdbcTemplate.update(updateSql, params.toArray());
    }

    public <T> int updateById(T updateObj, Serializable id) {
        if (QueryUtil.isNull(updateObj) || QueryUtil.isIllegalId(id)) {
            return 0;
        }

        Class<?> clazz = updateObj.getClass();
        Table table = tcInfo.findTableByClass(clazz);
        if (QueryUtil.isNull(table)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("update: class({}) has no table defined", clazz.getName());
            }
            return 0;
        }
        return update(updateObj, SingleTableWhere.buildId(table.idWhere(false), id));
    }

    public <T> int updateByIds(T updateObj, List<Serializable> ids) {
        if (QueryUtil.isNull(updateObj) || QueryUtil.isIllegalIdList(ids)) {
            return 0;
        }

        Class<?> clazz = updateObj.getClass();
        Table table = tcInfo.findTableByClass(clazz);
        if (QueryUtil.isNull(table)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("update: class({}) has no table defined", clazz.getName());
            }
            return 0;
        }
        return update(updateObj, SingleTableWhere.buildIds(table.idWhere(false), ids));
    }

    public <T> int update(T updateObj, SingleTableWhere query) {
        return update(updateObj, query, false);
    }

    public <T> int update(T updateObj, SingleTableWhere query, boolean generateNullField) {
        if (QueryUtil.isNull(updateObj) || QueryUtil.isNull(query)) {
            return 0;
        }

        Class<?> clazz = updateObj.getClass();
        Table table = tcInfo.findTableByClass(clazz);
        if (QueryUtil.isNull(table)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("update: class({}) has no table defined", clazz.getName());
            }
            return 0;
        }

        List<Object> params = new ArrayList<>();
        String updateSql = table.generateUpdate(updateObj, generateNullField, query, tcInfo, params);
        if (QueryUtil.isEmpty(updateSql) || QueryUtil.isEmpty(params)) {
            return 0;
        }
        return jdbcTemplate.update(updateSql, params.toArray());
    }


    public Object query(RequestInfo req) {
        req.checkTable(tcInfo);
        Set<String> paramTableSet = req.checkParam(tcInfo, maxListCount);

        Set<String> allResultTableSet = new LinkedHashSet<>();
        Set<String> resultFuncTableSet = req.checkResult(tcInfo, allResultTableSet);

        List<TableJoinRelation> allRelationList = req.allRelationList(tcInfo);
        Set<String> allTableSet = calcTableSet(allRelationList);
        req.checkAllTable(tcInfo, allTableSet, paramTableSet, allResultTableSet);

        String mainTable = req.getTable();
        ReqParam param = req.getParam();
        ReqResult result = req.getResult();

        String allFromSql = QuerySqlUtil.toFromSql(tcInfo, mainTable, allRelationList);
        List<Object> params = new ArrayList<>();
        if (param.needQueryPage()) {
            if (param.needQueryCount()) {
                List<TableJoinRelation> paramRelationList = req.paramRelationList(tcInfo, paramTableSet, resultFuncTableSet);
                Set<String> queryTableSet = calcTableSet(paramRelationList);
                boolean queryHasMany = calcQueryHasMany(paramRelationList);

                String firstFromSql = QuerySqlUtil.toFromSql(tcInfo, mainTable, paramRelationList);
                String whereSql = QuerySqlUtil.toWhereSql(tcInfo, mainTable, !queryTableSet.isEmpty(), param, params);
                return queryPage(firstFromSql, allFromSql, whereSql, mainTable, param, result, queryHasMany, queryTableSet, allTableSet, params);
            } else {
                String whereSql = QuerySqlUtil.toWhereSql(tcInfo, mainTable, !allTableSet.isEmpty(), param, params);
                return queryList(allFromSql + whereSql, mainTable, param, result, allTableSet, params);
            }
        } else {
            String whereSql = QuerySqlUtil.toWhereSql(tcInfo, mainTable, !allTableSet.isEmpty(), param, params);
            if (req.getType() == ResultType.OBJ) {
                return queryObj(allFromSql + whereSql, mainTable, param, result, allTableSet, params);
            } else {
                return queryListNoLimit(allFromSql + whereSql, mainTable, param, result, allTableSet, params);
            }
        }
    }

    private Set<String> calcTableSet(List<TableJoinRelation> relationList) {
        Set<String> tableSet = new HashSet<>();
        for (TableJoinRelation joinRelation : relationList) {
            tableSet.add(joinRelation.getMasterTable().getName());
            tableSet.add(joinRelation.getChildTable().getName());
        }
        return tableSet;
    }
    private boolean calcQueryHasMany(List<TableJoinRelation> paramRelationList) {
        for (TableJoinRelation joinRelation : paramRelationList) {
            String masterTableName = joinRelation.getMasterTable().getName();
            String childTableName = joinRelation.getChildTable().getName();
            TableColumnRelation relation = tcInfo.findRelationByMasterChild(masterTableName, childTableName);
            if (QueryUtil.isNotNull(relation) && relation.getType().hasMany()) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> queryPage(String firstFromSql, String allFromSql, String whereSql, String mainTable,
                                          ReqParam param, ReqResult result, boolean queryHasMany,
                                          Set<String> queryTableSet, Set<String> allTableSet, List<Object> params) {
        String fromAndWhere = firstFromSql + whereSql;
        long count;
        List<Map<String, Object>> pageList;
        if (result.needGroup()) {
            // SELECT COUNT(*) FROM ( SELECT ... FROM ... WHERE .?. GROUP BY ... HAVING ... ) tmp    (only where's table)
            String selectCountGroupSql = QuerySqlUtil.toSelectGroupSql(tcInfo, fromAndWhere, mainTable, result, queryTableSet, params);
            count = queryCount(QuerySqlUtil.toCountGroupSql(selectCountGroupSql), params);
            if (param.needQueryCurrentPage(count)) {
                String fromAndWhereList = allFromSql + whereSql;
                // SELECT ... FROM ... WHERE .?. GROUP BY ... HAVING ... LIMIT ...    (all where's table)
                String selectListGroupSql = QuerySqlUtil.toSelectGroupSql(tcInfo, fromAndWhereList, mainTable, result, allTableSet, params);
                pageList = queryPageListWithGroup(selectListGroupSql, mainTable, allTableSet, param, result, params);
            } else {
                pageList = Collections.emptyList();
            }
        } else {
            boolean needAlias = !queryTableSet.isEmpty();
            // SELECT COUNT(DISTINCT id) FROM ... WHERE .?..   (only where's table)
            String countSql = QuerySqlUtil.toCountWithoutGroupSql(tcInfo, mainTable, needAlias, queryHasMany, fromAndWhere);
            count = queryCount(countSql, params);
            if (param.needQueryCurrentPage(count)) {
                pageList = queryPageListWithoutGroup(firstFromSql, allFromSql, whereSql, mainTable, param,
                        result, needAlias, allTableSet, params);
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
        return QueryUtil.isNull(count) ? 0L : count;
    }

    private List<Map<String, Object>> queryPageListWithoutGroup(String firstFromSql, String allFromSql,
                                                                String whereSql, String mainTable, ReqParam param,
                                                                ReqResult result, boolean needAlias,
                                                                Set<String> allTableSet, List<Object> params) {
        String fromAndWhere = firstFromSql + whereSql;
        String sql;
        // deep paging(need offset a lot of result), use 「where + order + limit」 to query id, then use id to query specific columns
        if (param.hasDeepPage(deepMaxPageSize)) {
            // SELECT id FROM ... WHERE .?. ORDER BY ... LIMIT ...   (only where's table)
            String idPageSql = QuerySqlUtil.toIdPageSql(tcInfo, fromAndWhere, mainTable, needAlias, param, params);
            List<Map<String, Object>> idList = jdbcTemplate.queryForList(idPageSql, params.toArray());

            // SELECT ... FROM .?. WHERE id IN (...)    (all where's table)
            params.clear();
            sql = QuerySqlUtil.toSelectWithIdSql(tcInfo, mainTable, allFromSql, result, idList, allTableSet, params);
        } else {
            // SELECT ... FROM ... WHERE ... ORDER BY ... limit ...
            sql = QuerySqlUtil.toPageWithoutGroupSql(tcInfo, fromAndWhere, mainTable, param, result, allTableSet, params);
        }
        return assemblyResult(sql, params, mainTable, allTableSet, result);
    }

    private List<Map<String, Object>> queryPageListWithGroup(String selectGroupSql, String mainTable, Set<String> allTableSet,
                                                             ReqParam param, ReqResult result, List<Object> params) {
        String sql = selectGroupSql + param.generatePageSql(params);
        return assemblyResult(sql, params, mainTable, allTableSet, result);
    }

    private List<Map<String, Object>> queryList(String fromAndWhere, String mainTable, ReqParam param,
                                                ReqResult result, Set<String> allTableSet, List<Object> params) {
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(tcInfo, fromAndWhere, mainTable, result, allTableSet, params);
        String orderSql = param.generateOrderSql(mainTable, !allTableSet.isEmpty(), tcInfo);
        String sql = selectGroupSql + orderSql + param.generatePageSql(params);
        return assemblyResult(sql, params, mainTable, allTableSet, result);
    }

    private List<Map<String, Object>> queryListNoLimit(String fromAndWhere, String mainTable, ReqParam param,
                                                       ReqResult result, Set<String> allTableSet, List<Object> params) {
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(tcInfo, fromAndWhere, mainTable, result, allTableSet, params);
        String orderSql = param.generateOrderSql(mainTable, !allTableSet.isEmpty(), tcInfo);
        String sql = selectGroupSql + orderSql;
        return assemblyResult(sql, params, mainTable, allTableSet, result);
    }

    private Map<String, Object> queryObj(String fromAndWhere, String mainTable, ReqParam param, ReqResult result,
                                         Set<String> allTableSet, List<Object> params) {
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(tcInfo, fromAndWhere, mainTable, result, allTableSet, params);
        String orderSql = param.generateOrderSql(mainTable, !allTableSet.isEmpty(), tcInfo);
        String sql = selectGroupSql + orderSql + param.generateArrToObjSql(params);
        Map<String, Object> obj = QueryUtil.first(assemblyResult(sql, params, mainTable, allTableSet, result));
        return QueryUtil.isNull(obj) ? Collections.emptyMap() : obj;
    }

    private List<Map<String, Object>> assemblyResult(String mainSql, List<Object> params, String mainTable,
                                                     Set<String> allTableSet, ReqResult result) {
        List<Map<String, Object>> dataList = jdbcTemplate.queryForList(mainSql, params.toArray());
        if (QueryUtil.isNotEmpty(dataList)) {
            Table table = tcInfo.findTable(mainTable);
            String mainTableName = table.getName();

            Set<String> selectColumnSet = result.selectColumn(mainTable, tcInfo, allTableSet);
            List<String> removeColumnList = new ArrayList<>();
            for (String ic : result.innerColumn(mainTable, tcInfo, !allTableSet.isEmpty())) {
                if (!selectColumnSet.contains(ic)) {
                    removeColumnList.add(ic);
                }
            }
            for (Map<String, Object> data : dataList) {
                removeColumnList.forEach(data::remove);
                result.handleDateType(data, mainTable, tcInfo);
            }

            // order_address.order_id : order.id    +    order_item.code : order.code
            Map<String, ReqResult> innerResultMap = result.innerResult();
            if (QueryUtil.isNotEmpty(innerResultMap)) {
                // { address : id, items : code }
                Map<String, String> innerColumnMap = new HashMap<>();
                //  { address : { id1 : { ... },  id2 : { ... } }, items : { code1 : [ ... ], code2 : [ ... ] } }
                Map<String, Map<String, Object>> innerDataMap = new HashMap<>();
                for (Map.Entry<String, ReqResult> entry : innerResultMap.entrySet()) {
                    String fieldName = entry.getKey();
                    // { id : { id1 : { ... },  id2 : { ... } } }    or    { code : { code1 : [ ... ], code2 : [ ... ] } }
                    Map<String, Map<String, Object>> valueMap = queryInnerData(mainTableName, entry.getValue());
                    if (QueryUtil.isNotEmpty(valueMap)) {
                        for (Map.Entry<String, Map<String, Object>> valueEntry : valueMap.entrySet()) {
                            innerColumnMap.put(fieldName, valueEntry.getKey());
                            innerDataMap.put(fieldName, valueEntry.getValue());
                        }
                    }
                }
                for (Map<String, Object> data : dataList) {
                    for (Map.Entry<String, Map<String, Object>> entry : innerDataMap.entrySet()) {
                        // address    or    items
                        String fieldName = entry.getKey();
                        // id    or    code
                        String columnName = innerColumnMap.get(fieldName);
                        // id1, id2    or    code1, code2
                        Object relationValue = data.get(columnName);
                        if (QueryUtil.isNotNull(relationValue)) {
                            // { id1 : { ... },  id2 : { ... } }    or    { code1 : [ ... ], code2 : [ ... ] }
                            Map<String, Object> innerData = entry.getValue();
                            // put --> address : { ... }    or    items : [ ... ]
                            data.put(columnName, innerData.get(QueryUtil.toStr(relationValue)));
                        }
                    }
                }
            }
        }
        return dataList;
    }

    private Map<String, Map<String, Object>> queryInnerData(String mainTableName, ReqResult result) {
        // todo
        String columnName;
        Map<String, Object> innerDataMap = new HashMap<>();
        // child-master or master-child all need to query masterId
        String innerTable = result.getTable();
        TableColumnRelation masterChild = tcInfo.findRelationByMasterChild(mainTableName, innerTable);
        if (QueryUtil.isNotNull(masterChild)) {
            columnName = masterChild.getOneColumn();
            // master-child : SELECT * FROM t_inner where parent_id in ...
            List<Object> params = new ArrayList<>();
            String innerSql = "";
            List<Map<String, Object>> mapList = jdbcTemplate.queryForList(innerSql, params);
        } else {
            TableColumnRelation childMaster = tcInfo.findRelationByMasterChild(innerTable, mainTableName);
            if (QueryUtil.isNotNull(childMaster)) {
                columnName = childMaster.getOneColumn();
                // child-master : SELECT * FROM t_inner where parent_id in ...
                List<Object> params = new ArrayList<>();
                String innerSql = "";
                List<Map<String, Object>> mapList = jdbcTemplate.queryForList(innerSql, params);
            } else {
                columnName = "";
            }
        }
        if (QueryUtil.isEmpty(columnName) || QueryUtil.isEmpty(innerDataMap)) {
            return Collections.emptyMap();
        }

        Map<String, ReqResult> innerResultMap = result.innerResult();
        if (QueryUtil.isNotEmpty(innerResultMap)) {
            for (Map.Entry<String, ReqResult> entry : innerResultMap.entrySet()) {
                String fieldName = entry.getKey();
            }
        }

        // { id : { id1 : { ... },  id2 : { ... } } }    or    { code : { code1 : [ ... ], code2 : [ ... ] } }
        Map<String, Map<String, Object>> returnMap = new HashMap<>();
        returnMap.put(columnName, innerDataMap);
        return returnMap;
    }
}
