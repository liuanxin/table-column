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
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;

@SuppressWarnings({"DuplicatedCode"})
public class TableColumnTemplate implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(TableColumnTemplate.class);

    @Value("${query.online:false}")
    private boolean online;

    @Value("${query.scan-packages:}")
    private String scanPackages;

    @Value("${query.table-prefix:}")
    private String tablePrefix;

    @Value("${query.deep-max-page-size:10000}")
    private int deepMaxPageSize;

    @Value("${query.max-list-count:1000}")
    private int maxListCount;

    @Value("${query.one-to-one-has-many:0}")
    private int oneToOneHasMany;

    @Value("${query.logic-delete-column:}")
    private String logicDeleteColumn;

    @Value("${query.logic-value:}")
    private String logicValue;

    @Value("${query.logic-delete-boolean-value:}")
    private String logicDeleteBooleanValue;

    @Value("${query.logic-delete-int-value:}")
    private String logicDeleteIntValue;

    @Value("${query.logic-delete-long-value:}")
    private String logicDeleteLongValue;

    private TableColumnInfo tcInfo;

    private final JdbcTemplate jdbcTemplate;
    private final List<TableColumnRelation> relationList;
    public TableColumnTemplate(JdbcTemplate jdbcTemplate, List<TableColumnRelation> relationList) {
        this.jdbcTemplate = jdbcTemplate;
        this.relationList = QueryUtil.isEmpty(relationList) ? new ArrayList<>() : new ArrayList<>(relationList);
    }

    @Override
    public void afterPropertiesSet() {
        if (QueryUtil.isNotEmpty(scanPackages)) {
            tcInfo = QueryInfoUtil.infoWithScan(tablePrefix, scanPackages, relationList,
                    logicDeleteColumn, logicValue, logicDeleteBooleanValue, logicDeleteIntValue, logicDeleteLongValue);
            if (QueryUtil.isNull(tcInfo)) {
                throw new RuntimeException(String.format("class not found in(%s)", scanPackages));
            }
        } else {
            String dbName = jdbcTemplate.queryForObject(QueryConst.DB_SQL, String.class);
            // table_name, table_comment
            List<Map<String, Object>> tableList = jdbcTemplate.queryForList(QueryConst.TABLE_SQL, dbName);
            // table_name, column_name, column_type, column_comment, has_pri, varchar_length
            List<Map<String, Object>> tableColumnList = jdbcTemplate.queryForList(QueryConst.COLUMN_SQL, dbName);
            tcInfo = QueryInfoUtil.infoWithDb(tablePrefix, tableList, tableColumnList,
                    logicDeleteColumn, logicValue, logicDeleteBooleanValue, logicDeleteIntValue, logicDeleteLongValue);
        }
        QueryInfoUtil.checkAndSetRelation(relationList, tcInfo);
    }


    public TableColumnInfo getTcInfo() {
        return tcInfo;
    }


    public void generateModel(String tables, String targetPath, String packagePath) {
        generateModel(tables, targetPath, packagePath, false, "");
    }
    public void generateModel(String tables, String targetPath, String packagePath, String modelSuffix) {
        generateModel(tables, targetPath, packagePath, false, modelSuffix);
    }
    public void generateModel(String tables, String targetPath, String packagePath, boolean generateComment) {
        generateModel(tables, targetPath, packagePath, generateComment, "");
    }
    public void generateModel(String tables, String targetPath, String packagePath,
                              boolean generateComment, String modelSuffix) {
        String dbName = jdbcTemplate.queryForObject(QueryConst.DB_SQL, String.class);
        // table_name, table_comment
        List<Map<String, Object>> tableList = jdbcTemplate.queryForList(QueryConst.TABLE_SQL, dbName);
        // table_name, column_name, column_type, column_comment, has_pri, varchar_length
        List<Map<String, Object>> tableColumnList = jdbcTemplate.queryForList(QueryConst.COLUMN_SQL, dbName);
        Set<String> tableSet = new LinkedHashSet<>();
        if (QueryUtil.isNotEmpty(tables)) {
            for (String te : tables.split(",")) {
                String trim = te.trim();
                if (QueryUtil.isNotEmpty(trim)) {
                    tableSet.add(trim.toLowerCase());
                }
            }
        }
        QueryInfoUtil.generateModel(tableSet, targetPath, packagePath, modelSuffix,
                tablePrefix, generateComment, tableList, tableColumnList);
    }


    public List<QueryInfo> info(String tables) {
        if (online) {
            return Collections.emptyList();
        }

        Set<String> tableSet = new LinkedHashSet<>();
        if (QueryUtil.isNotEmpty(tables)) {
            for (String te : tables.split(",")) {
                String trim = te.trim();
                if (QueryUtil.isNotEmpty(trim)) {
                    tableSet.add(trim.toLowerCase());
                }
            }
        }
        List<QueryInfo> queryList = new ArrayList<>();
        for (Table table : tcInfo.allTable()) {
            String tableAlias = table.getAlias();
            if (QueryUtil.isEmpty(tableSet) || tableSet.contains(tableAlias.toLowerCase())) {
                List<QueryInfo.QueryColumn> columnList = new ArrayList<>();
                for (TableColumn tc : table.getColumnMap().values()) {
                    String type = tc.getFieldType().getSimpleName();
                    Integer length = tc.getStrLen();
                    TableColumnRelation relation = tcInfo.findRelationByChild(table.getName(), tc.getName());
                    String relationTable, relationColumn;
                    if (QueryUtil.isNull(relation)) {
                        relationTable = null;
                        relationColumn = null;
                    } else {
                        Table tb = tcInfo.findTable(relation.getOneTable());
                        relationTable = tb.getAlias();
                        relationColumn = tb.getColumnMap().get(relation.getOneColumn()).getAlias();
                    }
                    boolean needValue = tc.isNotNull() && !tc.isHasDefault();
                    columnList.add(new QueryInfo.QueryColumn(tc.getAlias(), tc.getDesc(), type,
                            needValue, length, relationTable, relationColumn));
                }
                queryList.add(new QueryInfo(tableAlias, table.getDesc(), columnList));
            }
        }
        return queryList;
    }


    @Transactional
    public int insert(String table, Map<String, Object> data) {
        return insert(table, data, false);
    }

    @Transactional
    public int insert(String table, Map<String, Object> data, boolean generateNullField) {
        if (QueryUtil.isEmpty(table) || QueryUtil.isNotEmpty(data)) {
            return 0;
        }

        Table tableInfo = tcInfo.findTable(table.trim());
        if (QueryUtil.isNull(tableInfo)) {
            throw new RuntimeException("insert: table(" + table + ") has no defined");
        }

        Set<String> needCheckColumnSet = new HashSet<>();
        for (TableColumn tc : tableInfo.getColumnMap().values()) {
            if (tc.isNotNull() && !tc.isHasDefault()) {
                needCheckColumnSet.add(tc.getAlias());
            }
        }
        if (QueryUtil.isNotEmpty(needCheckColumnSet)) {
            List<String> columnList = new ArrayList<>();
            for (String alias : needCheckColumnSet) {
                if (QueryUtil.isNull(data.get(alias))) {
                    columnList.add(alias);
                }
            }
            if (QueryUtil.isNotEmpty(columnList)) {
                throw new RuntimeException("insert: table(" + table + ") columns" + QueryUtil.toStr(columnList) + " can't be null");
            }
        }

        StringBuilder printSql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        String insertSql = tableInfo.generateInsertMap(data, generateNullField, params, printSql);
        if (QueryUtil.isEmpty(insertSql)) {
            return 0;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("insert sql: [{}]", printSql);
        }
        return jdbcTemplate.update(insertSql, params.toArray());
    }

    @Transactional
    public int insertBatch(String table, List<Map<String, Object>> list) {
        return insertBatch(table, list, 500);
    }

    @Transactional
    public int insertBatch(String table, List<Map<String, Object>> list, int singleCount) {
        return insertBatch(table, list, singleCount, false);
    }

    @Transactional
    public int insertBatch(String table, List<Map<String, Object>> list, int singleCount, boolean generateNullField) {
        if (QueryUtil.isEmpty(table) || QueryUtil.isEmpty(list)) {
            return 0;
        }

        Table tableInfo = tcInfo.findTable(table);
        if (QueryUtil.isNull(tableInfo)) {
            throw new RuntimeException("batch insert-map: table(" + table + ") has no defined");
        }

        Set<String> needCheckColumnSet = new HashSet<>();
        for (TableColumn tc : tableInfo.getColumnMap().values()) {
            if (tc.isNotNull() && !tc.isHasDefault()) {
                needCheckColumnSet.add(tc.getAlias());
            }
        }
        if (QueryUtil.isNotEmpty(needCheckColumnSet)) {
            Map<Integer, List<String>> columnMap = new LinkedHashMap<>();
            int size = list.size();
            for (String alias : needCheckColumnSet) {
                for (int i = 0; i < size; i++) {
                    if (QueryUtil.isNull(list.get(i).get(alias))) {
                        columnMap.computeIfAbsent(i + 1, (k) -> new ArrayList<>()).add(alias);
                    }
                }
            }
            if (QueryUtil.isNotEmpty(columnMap)) {
                throw new RuntimeException("batch insert-map: table(" + table + ") " + QueryUtil.toStr(columnMap) + " can't be null");
            }
        }

        int flag = 0;
        for (List<Map<String, Object>> lt : QueryUtil.split(list, singleCount)) {
            StringBuilder printSql = new StringBuilder();
            List<Object> params = new ArrayList<>();
            String batchInsertSql = tableInfo.generateBatchInsertMap(lt, generateNullField, params, printSql);
            if (QueryUtil.isNotEmpty(batchInsertSql)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("batch insert-map sql: [{}]", printSql);
                }
                flag += jdbcTemplate.update(batchInsertSql, params.toArray());
            }
        }
        return flag;
    }

    @Transactional
    public <T> int insert(T obj) {
        return insert(obj, false);
    }

    @Transactional
    public <T> int insert(T obj, boolean generateNullField) {
        if (QueryUtil.isNull(obj)) {
            return 0;
        }

        Class<?> clazz = obj.getClass();
        Table table = tcInfo.findTableByClass(clazz);
        if (QueryUtil.isNull(table)) {
            throw new RuntimeException("insert: table(" + clazz + ") has no defined");
        }

        Set<String> needCheckFieldSet = new HashSet<>();
        for (TableColumn tc : table.getColumnMap().values()) {
            if (tc.isNotNull() && !tc.isHasDefault()) {
                needCheckFieldSet.add(tc.getFieldName());
            }
        }
        if (QueryUtil.isNotEmpty(needCheckFieldSet)) {
            List<String> nullColumnList = new ArrayList<>();
            List<String> errorList = new ArrayList<>();
            for (String field : needCheckFieldSet) {
                try {
                    if (QueryUtil.isNull(QueryUtil.getFieldData(clazz, field, obj))) {
                        nullColumnList.add(field);
                    }
                } catch (IllegalAccessException e) {
                    errorList.add(field);
                }
            }
            if (QueryUtil.isNotEmpty(nullColumnList)) {
                throw new RuntimeException("insert: table(" + clazz + ") field" + QueryUtil.toStr(nullColumnList) + " can't be null");
            }
            if (QueryUtil.isNotEmpty(errorList)) {
                throw new RuntimeException("insert: table(" + clazz + ") get field" + QueryUtil.toStr(errorList) + " data error");
            }
        }

        StringBuilder printSql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        String insertSql = table.generateInsert(obj, generateNullField, params, printSql);
        if (QueryUtil.isEmpty(insertSql)) {
            return 0;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("insert sql: [{}]", printSql);
        }
        return jdbcTemplate.update(insertSql, params.toArray());
    }

    @Transactional
    public <T> int insertBatch(List<T> list) {
        return insertBatch(list, 500);
    }

    @Transactional
    public <T> int insertBatch(List<T> list, int singleCount) {
        return insertBatch(list, singleCount, false);
    }

    @Transactional
    public <T> int insertBatch(List<T> list, int singleCount, boolean generateNullField) {
        if (QueryUtil.isEmpty(list)) {
            return 0;
        }

        Class<?> clazz = list.get(0).getClass();
        Table table = tcInfo.findTableByClass(clazz);
        if (QueryUtil.isNull(table)) {
            throw new RuntimeException("batch insert: table(" + clazz + ") has no defined");
        }

        Set<String> needCheckFieldSet = new HashSet<>();
        for (TableColumn tc : table.getColumnMap().values()) {
            if (tc.isNotNull() && !tc.isHasDefault()) {
                needCheckFieldSet.add(tc.getFieldName());
            }
        }
        if (QueryUtil.isNotEmpty(needCheckFieldSet)) {
            Map<Integer, List<String>> nullColumnMap = new LinkedHashMap<>();
            Map<Integer, List<String>> errorColumnMap = new LinkedHashMap<>();
            int size = list.size();
            for (String field : needCheckFieldSet) {
                for (int i = 0; i < size; i++) {
                    int index = i + 1;
                    try {
                        if (QueryUtil.isNull(QueryUtil.getFieldData(clazz, field, list.get(i)))) {
                            nullColumnMap.computeIfAbsent(index, (k) -> new ArrayList<>()).add(field);
                        }
                    } catch (IllegalAccessException e) {
                        errorColumnMap.computeIfAbsent(index, (k) -> new ArrayList<>()).add(field);
                    }
                }
            }
            if (QueryUtil.isNotEmpty(nullColumnMap)) {
                throw new RuntimeException("batch insert: table(" + table + ") " + QueryUtil.toStr(nullColumnMap) + " can't be null");
            }
            if (QueryUtil.isNotEmpty(errorColumnMap)) {
                throw new RuntimeException("batch insert: table(" + table + ") get field " + QueryUtil.toStr(errorColumnMap) + " data error");
            }
        }

        int flag = 0;
        for (List<T> lt : QueryUtil.split(list, singleCount)) {
            StringBuilder printSql = new StringBuilder();
            List<Object> params = new ArrayList<>();
            String batchInsertSql = table.generateBatchInsert(lt, generateNullField, params, printSql);
            if (QueryUtil.isNotEmpty(batchInsertSql)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("batch insert sql: [{}]", printSql);
                }
                flag += jdbcTemplate.update(batchInsertSql, params.toArray());
            }
        }
        return flag;
    }


    @Transactional
    public int forceDeleteById(String table, Serializable id) {
        return deleteById(table, id, true);
    }
    private int deleteById(String table, Serializable id, boolean force) {
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
        return doDelete(SingleTableWhere.buildId(tableInfo.idWhere(false), id), tableInfo, force);
    }
    @Transactional
    public int deleteById(String table, Serializable id) {
        return deleteById(table, id, false);
    }

    @Transactional
    public int forceDeleteByIds(String table, List<Serializable> ids) {
        return deleteByIds(table, ids, true);
    }
    private int deleteByIds(String table, List<Serializable> ids, boolean force) {
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
        return doDelete(SingleTableWhere.buildIds(tableInfo.idWhere(false), ids), tableInfo, force);
    }
    @Transactional
    public int deleteByIds(String table, List<Serializable> ids) {
        return deleteByIds(table, ids, false);
    }

    @Transactional
    public int forceDelete(String table, SingleTableWhere query) {
        return delete(table, query, true);
    }
    private int delete(String table, SingleTableWhere query, boolean force) {
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
        return doDelete(query, tableInfo, force);
    }
    @Transactional
    public int delete(String table, SingleTableWhere query) {
        return delete(table, query, false);
    }

    @Transactional
    public <T> int forceDeleteById(Class<T> clazz, Serializable id) {
        return deleteById(clazz, id, true);
    }
    private <T> int deleteById(Class<T> clazz, Serializable id, boolean force) {
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
        return doDelete(SingleTableWhere.buildId(table.idWhere(false), id), table, force);
    }
    @Transactional
    public <T> int deleteById(Class<T> clazz, Serializable id) {
        return deleteById(clazz, id, false);
    }

    @Transactional
    public <T> int forceDeleteByIds(Class<T> clazz, List<Serializable> ids) {
        return deleteByIds(clazz, ids, true);
    }
    private <T> int deleteByIds(Class<T> clazz, List<Serializable> ids, boolean force) {
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
        return doDelete(SingleTableWhere.buildIds(table.idWhere(false), ids), table, force);
    }
    @Transactional
    public <T> int deleteByIds(Class<T> clazz, List<Serializable> ids) {
        return deleteByIds(clazz, ids, false);
    }

    @Transactional
    public <T> int forceDelete(Class<T> clazz, SingleTableWhere query) {
        return delete(clazz, query, true);
    }
    private <T> int delete(Class<T> clazz, SingleTableWhere query, boolean force) {
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
        return doDelete(query, table, force);
    }
    @Transactional
    public <T> int delete(Class<T> clazz, SingleTableWhere query) {
        return delete(clazz, query, false);
    }

    private int doDelete(SingleTableWhere query, Table table, boolean force) {
        StringBuilder printSql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        String deleteSql = table.generateDelete(query, tcInfo, params, printSql, force);
        if (QueryUtil.isEmpty(deleteSql)) {
            return 0;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("delete sql: [{}]", printSql);
        }
        return jdbcTemplate.update(deleteSql, params.toArray());
    }


    @Transactional
    public int updateById(String table, Map<String, Object> updateObj, Serializable id) {
        if (QueryUtil.isEmpty(table) || QueryUtil.isEmpty(updateObj) || QueryUtil.isIllegalId(id)) {
            return 0;
        }

        Table tableInfo = tcInfo.findTable(table.trim());
        if (QueryUtil.isNull(tableInfo)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("update-map: has no table({}) defined", table);
            }
            return 0;
        }
        return update(table, updateObj, SingleTableWhere.buildId(tableInfo.idWhere(false), id));
    }

    @Transactional
    public int updateByIds(String table, Map<String, Object> updateObj, List<Serializable> ids) {
        if (QueryUtil.isEmpty(table) || QueryUtil.isEmpty(updateObj) || QueryUtil.isIllegalIdList(ids)) {
            return 0;
        }

        Table tableInfo = tcInfo.findTable(table.trim());
        if (QueryUtil.isNull(tableInfo)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("update-map: has no table({}) defined", table);
            }
            return 0;
        }
        return update(table, updateObj, SingleTableWhere.buildIds(tableInfo.idWhere(false), ids));
    }

    @Transactional
    public int update(String table, Map<String, Object> updateObj, SingleTableWhere query) {
        return update(table, updateObj, query, false);
    }

    @Transactional
    public int update(String table, Map<String, Object> updateObj, SingleTableWhere query, boolean generateNullField) {
        if (QueryUtil.isEmpty(table) || QueryUtil.isEmpty(updateObj) || QueryUtil.isNull(query)) {
            return 0;
        }

        Table tableInfo = tcInfo.findTable(table.trim());
        if (QueryUtil.isNull(tableInfo)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("update-map: has no table({}) defined", table);
            }
            return 0;
        }

        StringBuilder printSql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        String updateSql = tableInfo.generateUpdateMap(updateObj, generateNullField, query, tcInfo, params, printSql);
        if (QueryUtil.isEmpty(updateSql)) {
            return 0;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("update-map sql: [{}]", printSql);
        }
        return jdbcTemplate.update(updateSql, params.toArray());
    }

    @Transactional
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

    @Transactional
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

    @Transactional
    public <T> int update(T updateObj, SingleTableWhere query) {
        return update(updateObj, query, false);
    }

    @Transactional
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

        StringBuilder printSql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        String updateSql = table.generateUpdate(updateObj, generateNullField, query, tcInfo, params, printSql);
        if (QueryUtil.isEmpty(updateSql)) {
            return 0;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("update sql: [{}]", printSql);
        }
        return jdbcTemplate.update(updateSql, params.toArray());
    }


    public Map<String, Object> forceQueryById(String table, Serializable id) {
        return queryById(table, id, true);
    }
    private Map<String, Object> queryById(String table, Serializable id, boolean force) {
        if (QueryUtil.isEmpty(table) || QueryUtil.isIllegalId(id)) {
            return Collections.emptyMap();
        }

        Table tableInfo = tcInfo.findTable(table.trim());
        if (QueryUtil.isNull(tableInfo)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("query: has no table({}) defined", table);
            }
            return Collections.emptyMap();
        }

        StringBuilder printSql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        SingleTableWhere query = SingleTableWhere.buildId(tableInfo.idWhere(false), id);
        String querySql = tableInfo.generateQuery(query, tcInfo, params, printSql, tableInfo.generateSelect(true),
                null, null, null, null, QueryConst.LIMIT_ONE, force);
        if (QueryUtil.isEmpty(querySql)) {
            return Collections.emptyMap();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("query sql: [{}]", printSql);
        }
        return QueryUtil.first(jdbcTemplate.queryForList(querySql, params.toArray()));
    }
    public Map<String, Object> queryById(String table, Serializable id) {
        return queryById(table, id, false);
    }

    public List<Map<String, Object>> forceQueryByIds(String table, List<Serializable> ids) {
        return queryByIds(table, ids, true);
    }
    private List<Map<String, Object>> queryByIds(String table, List<Serializable> ids, boolean force) {
        if (QueryUtil.isEmpty(table) || QueryUtil.isIllegalIdList(ids)) {
            return Collections.emptyList();
        }

        Table tableInfo = tcInfo.findTable(table.trim());
        if (QueryUtil.isNull(tableInfo)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("query: has no table({}) defined", table);
            }
            return Collections.emptyList();
        }

        String idField = tableInfo.idWhere(false);
        String select = tableInfo.generateSelect(true);
        List<Map<String, Object>> returnList = new ArrayList<>();
        for (List<Serializable> lt : QueryUtil.split(ids, maxListCount)) {
            StringBuilder printSql = new StringBuilder();
            List<Object> params = new ArrayList<>();
            SingleTableWhere query = SingleTableWhere.buildIds(idField, lt);
            String querySql = tableInfo.generateQuery(query, tcInfo, params, printSql, select,
                    null, null, null, null, null, force);
            if (QueryUtil.isNotEmpty(querySql)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("query sql: [{}]", printSql);
                }
                List<Map<String, Object>> maps = jdbcTemplate.queryForList(querySql, params.toArray());
                if (QueryUtil.isNotEmpty(maps)) {
                    returnList.addAll(maps);
                }
            }
        }
        return returnList;
    }
    public List<Map<String, Object>> queryByIds(String table, List<Serializable> ids) {
        return queryByIds(table, ids, false);
    }

    public List<Map<String, Object>> forceQuery(String table, SingleTableWhere query) {
        return query(table, query, null, null, null, null, null, true);
    }
    public List<Map<String, Object>> query(String table, SingleTableWhere query) {
        return query(table, query, null, null, null, null, null, false);
    }
    public List<Map<String, Object>> query(String table, SingleTableWhere query, String groupBy, String having,
                                           String havingPrint, String orderBy, List<Integer> pageList, boolean force) {
        if (QueryUtil.isEmpty(table) || QueryUtil.isNull(query)) {
            return Collections.emptyList();
        }

        Table tableInfo = tcInfo.findTable(table);
        if (QueryUtil.isNull(tableInfo)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("query: has no table({}) defined", table);
            }
            return Collections.emptyList();
        }

        StringBuilder printSql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        String querySql = tableInfo.generateQuery(query, tcInfo, params, printSql, tableInfo.generateSelect(false),
                groupBy, having, havingPrint, orderBy, pageList, force);
        if (QueryUtil.isEmpty(querySql)) {
            return Collections.emptyList();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("query sql: [{}]", printSql);
        }
        return jdbcTemplate.queryForList(querySql, params.toArray());
    }
    public Map<String, Object> forceQueryOne(String table, SingleTableWhere query) {
        return QueryUtil.first(query(table, query, null, null, null, null, QueryConst.LIMIT_ONE, true));
    }
    public Map<String, Object> queryOne(String table, SingleTableWhere query) {
        return QueryUtil.first(query(table, query, null, null, null, null, QueryConst.LIMIT_ONE, false));
    }

    public long forceQueryCount(String table, SingleTableWhere query) {
        return queryCount(table, query, true);
    }
    private long queryCount(String table, SingleTableWhere query, boolean force) {
        if (QueryUtil.isEmpty(table) || QueryUtil.isNull(query)) {
            return 0;
        }

        Table tableInfo = tcInfo.findTable(table);
        if (QueryUtil.isNull(tableInfo)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("query: has no table({}) defined", table);
            }
            return 0;
        }

        StringBuilder printSql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        String querySql = tableInfo.generateCountQuery(query, tcInfo, params, printSql, null, null, null, null, null, force);
        if (QueryUtil.isEmpty(querySql)) {
            return 0;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("query count sql: [{}]", printSql);
        }
        Long count = jdbcTemplate.queryForObject(querySql, Long.class, params.toArray());
        return QueryUtil.isNull(count) ? 0 : count;
    }
    public long queryCount(String table, SingleTableWhere query) {
        return queryCount(table, query, false);
    }

    public <T> T forceQueryById(Class<T> clazz, Serializable id) {
        return queryById(clazz, id, true);
    }
    private <T> T queryById(Class<T> clazz, Serializable id, boolean force) {
        if (QueryUtil.isNull(clazz) || QueryUtil.isIllegalId(id)) {
            return null;
        }

        Table table = tcInfo.findTableByClass(clazz);
        if (QueryUtil.isNull(table)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("query: class({}) has no table defined", clazz.getName());
            }
            return null;
        }

        StringBuilder printSql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        SingleTableWhere query = SingleTableWhere.buildId(table.idWhere(false), id);
        String querySql = table.generateQuery(query, tcInfo, params, printSql, table.generateSelect(false),
                null, null, null, null, QueryConst.LIMIT_ONE, force);
        if (QueryUtil.isEmpty(querySql)) {
            return null;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("query-id sql: [{}]", printSql);
        }
        return QueryUtil.first(jdbcTemplate.queryForList(querySql, clazz, params.toArray()));
    }
    public <T> T queryById(Class<T> clazz, Serializable id) {
        return queryById(clazz, id, false);
    }

    public <T> List<T> forceQueryByIds(Class<T> clazz, List<Serializable> ids) {
        return queryByIds(clazz, ids, true);
    }
    private <T> List<T> queryByIds(Class<T> clazz, List<Serializable> ids, boolean force) {
        if (QueryUtil.isNull(clazz) || QueryUtil.isIllegalIdList(ids)) {
            return Collections.emptyList();
        }

        Table table = tcInfo.findTableByClass(clazz);
        if (QueryUtil.isNull(table)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("query: class({}) no table defined", clazz.getName());
            }
            return Collections.emptyList();
        }

        StringBuilder printSql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        SingleTableWhere query = SingleTableWhere.buildIds(table.idWhere(false), ids);
        String querySql = table.generateQuery(query, tcInfo, params, printSql, table.generateSelect(false),
                null, null, null, null, null, force);
        if (QueryUtil.isEmpty(querySql)) {
            return Collections.emptyList();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("query-ids sql: [{}]", printSql);
        }
        return jdbcTemplate.queryForList(querySql, clazz, params.toArray());
    }
    public <T> List<T> queryByIds(Class<T> clazz, List<Serializable> ids) {
        return queryByIds(clazz, ids, false);
    }

    public <T> List<T> forceQuery(Class<T> clazz, SingleTableWhere query) {
        return query(clazz, query, null, null, null, null, null, true);
    }
    public <T> List<T> query(Class<T> clazz, SingleTableWhere query) {
        return query(clazz, query, null, null, null, null, null, false);
    }
    public <T> List<T> query(Class<T> clazz, SingleTableWhere query, String groupBy, String having, String havingPrint,
                             String orderBy, List<Integer> pageList, boolean force) {
        if (QueryUtil.isNull(clazz) || QueryUtil.isNull(query)) {
            return Collections.emptyList();
        }

        Table table = tcInfo.findTableByClass(clazz);
        if (QueryUtil.isNull(table)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("query: class({}) has no table defined", clazz.getName());
            }
            return Collections.emptyList();
        }

        StringBuilder printSql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        String querySql = table.generateQuery(query, tcInfo, params, printSql, table.generateSelect(false),
                groupBy, having, havingPrint, orderBy, pageList, force);
        if (QueryUtil.isEmpty(querySql)) {
            return Collections.emptyList();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("query sql: [{}]", printSql);
        }
        return jdbcTemplate.queryForList(querySql, clazz, params.toArray());
    }
    public <T> T forceQueryOne(Class<T> clazz, SingleTableWhere query) {
        return QueryUtil.first(query(clazz, query, null, null, null, null, QueryConst.LIMIT_ONE, true));
    }
    public <T> T queryOne(Class<T> clazz, SingleTableWhere query) {
        return QueryUtil.first(query(clazz, query, null, null, null, null, QueryConst.LIMIT_ONE, false));
    }

    public <T> long forceQueryCount(Class<T> clazz, SingleTableWhere query) {
        return queryCount(clazz, query, true);
    }
    private <T> long queryCount(Class<T> clazz, SingleTableWhere query, boolean force) {
        if (QueryUtil.isNull(clazz) || QueryUtil.isNull(query)) {
            return 0;
        }

        Table table = tcInfo.findTableByClass(clazz);
        if (QueryUtil.isNull(table)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("query: class({}) has no table defined", clazz.getName());
            }
            return 0;
        }

        StringBuilder printSql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        String querySql = table.generateCountQuery(query, tcInfo, params, printSql, null, null, null, null, null, force);
        if (QueryUtil.isEmpty(querySql)) {
            return 0;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("query count sql: [{}]", printSql);
        }
        Long count = jdbcTemplate.queryForObject(querySql, Long.class, params.toArray());
        return QueryUtil.isNull(count) ? 0 : count;
    }
    public <T> long queryCount(Class<T> clazz, SingleTableWhere query) {
        return queryCount(clazz, query, false);
    }


    public Object forceDynamicQuery(RequestInfo req) {
        return dynamicQueryInfo(req, true);
    }

    public Object dynamicQuery(RequestInfo req) {
        return dynamicQueryInfo(req, false);
    }

    private Object dynamicQueryInfo(RequestInfo req, boolean force) {
        if (QueryUtil.isNull(req)) {
            return null;
        }

        req.checkTable(tcInfo);
        Set<String> paramTableSet = req.checkParam(tcInfo, maxListCount);
        Set<String> resultTableSet = req.checkResult(tcInfo);
        List<TableJoinRelation> useRelationList = req.checkRelation(tcInfo, paramTableSet, resultTableSet);
        Set<String> useTableSet = calcTableSet(useRelationList);
        req.checkAllTable(tcInfo, useTableSet, paramTableSet, resultTableSet);

        String mainTable = req.getTable();
        ReqParam param = req.getParam();
        ReqResult result = req.getResult();
        boolean needAlias = QueryUtil.isNotEmpty(useTableSet);

        String fromSql = QuerySqlUtil.toFromSql(tcInfo, mainTable, useRelationList);
        List<Object> params = new ArrayList<>();
        StringBuilder wherePrint = new StringBuilder();
        String whereSql = param.generateWhereSql(mainTable, tcInfo, needAlias, params, force, wherePrint);
        String fromAndWhere = fromSql + whereSql;
        String fromAndWherePrint = fromSql + wherePrint;

        // + query page (include count)
        //   + group by
        //     1. query count
        //       > SELECT COUNT(*) FROM ( SELECT ... FROM ... WHERE ... GROUP BY ... HAVING ... ) TMP
        //     2. query list
        //       > SELECT ... FROM ... WHERE ... GROUP BY ... HAVING ... ORDER BY ... LIMIT ...
        //   + no group by
        //     1. query count
        //       > multiple table: SELECT COUNT(DISTINCT id) FROM ..
        //       > single table:   SELECT COUNT(*) FROM ...
        //     2. query list
        //       + query deep page
        //         2.1. SELECT id FROM ... WHERE ... ORDER BY ... LIMIT x, x
        //         2.2. SELECT ... FROM ... WHERE id IN (...)
        //       + query no deep page
        //         > SELECT ... FROM ... WHERE ... LIMIT x, x
        //
        // + query page (exclusive count)
        //   > SELECT ... FROM ... WHERE ... GROUP BY ... HAVING ... ORDER BY ... LIMIT x, x
        //
        // + query one (no page)
        //   > SELECT ... FROM ... WHERE ... GROUP BY ... HAVING ... ORDER BY ... LIMIT 1
        //
        // + query list (no page)
        //   > SELECT ... FROM ... WHERE ... GROUP BY ... HAVING ... ORDER BY ...
        //
        // return data after assembly

        if (param.needQueryPage()) {
            if (param.needQueryCount()) {
                boolean queryHasMany = calcQueryHasMany(useRelationList);
                return queryCountPage(fromSql, whereSql, wherePrint.toString(), mainTable, param, result, queryHasMany, needAlias, params);
            } else {
                return queryNoCountPage(fromAndWhere, fromAndWherePrint, mainTable, param, result, needAlias, params);
            }
        } else {
            if (req.getType() == ResultType.OBJ) {
                return queryObj(fromAndWhere, fromAndWherePrint, mainTable, param, result, needAlias, params);
            } else {
                return queryList(fromAndWhere, fromAndWherePrint, mainTable, param, result, needAlias, params);
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

    private Map<String, Object> queryCountPage(String fromSql, String whereSql, String wherePrint, String mainTable,
                                               ReqParam param, ReqResult result, boolean queryHasMany,
                                               boolean needAlias, List<Object> params) {
        String fromAndWhere = fromSql + whereSql;
        String fromAndWherePrint = fromSql + wherePrint;
        long count;
        List<Map<String, Object>> pageList;
        StringBuilder countPrintSql = new StringBuilder();
        StringBuilder printSql = new StringBuilder();
        if (result.needGroup()) {
            // SELECT a.xx, b.yy, COUNT(*) cnt, MAX(a.xxx) max_xxx  FROM a inner join b on ...
            // WHERE ...  GROUP BY a.xx, b.yy  HAVING cnt > 1 AND max_xxx > 10
            String selectGroupSql = QuerySqlUtil.toSelectGroupSql(tcInfo, fromAndWhere,
                    fromAndWherePrint, mainTable, result, needAlias, params, printSql);
            // SELECT COUNT(*) FROM ( ^^^ SELECT FROM WHERE GROUP BY HAVING ^^^ ) tmp
            String selectGroupCountSql = QuerySqlUtil.toCountGroupSql(selectGroupSql, printSql.toString(), countPrintSql);
            count = queryCount(selectGroupCountSql, params, countPrintSql);
            if (param.needQueryCurrentPage(count)) {
                // ^^^ SELECT FROM WHERE GROUP BY HAVING ^^^ ORDER BY ... LIMIT ..
                pageList = queryPageListWithGroup(selectGroupSql, mainTable, needAlias, param, result, params, printSql);
            } else {
                pageList = Collections.emptyList();
            }
        } else {
            // SELECT COUNT(DISTINCT id) FROM ... WHERE ...
            String selectCountSql = QuerySqlUtil.toCountWithoutGroupSql(tcInfo, fromAndWhere,
                    fromAndWherePrint, mainTable, needAlias, queryHasMany, countPrintSql);
            count = queryCount(selectCountSql, params, countPrintSql);
            if (param.needQueryCurrentPage(count)) {
                pageList = queryLimitList(fromSql, whereSql, wherePrint, mainTable, param, result, needAlias, params, printSql);
            } else {
                pageList = Collections.emptyList();
            }
        }
        Map<String, Object> pageInfo = new LinkedHashMap<>();
        pageInfo.put("count", count);
        pageInfo.put("list", pageList);
        return pageInfo;
    }

    private long queryCount(String countSql, List<Object> params, StringBuilder printSql) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("query count sql: [{}]", printSql);
        }
        Long count = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        return QueryUtil.isNull(count) ? 0L : count;
    }

    private List<Map<String, Object>> queryLimitList(String fromSql, String whereSql, String wherePrint, String mainTable,
                                                     ReqParam param, ReqResult result, boolean needAlias,
                                                     List<Object> params, StringBuilder printSql) {
        String fromAndWhere = fromSql + whereSql;
        String fromAndWherePrint = fromSql + wherePrint;
        String sql;
        // deep paging(need offset a lot of result), use 「where + order + limit」 to query id, then use id to query specific columns
        if (param.hasDeepPage(deepMaxPageSize)) {
            // SELECT id FROM ... WHERE .?. ORDER BY ... LIMIT ...
            String idPageSql = QuerySqlUtil.toIdPageSql(tcInfo, fromAndWhere,
                    fromAndWherePrint, mainTable, needAlias, param, params, printSql);
            if (LOG.isDebugEnabled()) {
                LOG.debug("query condition sql: [{}]", printSql);
            }
            List<Map<String, Object>> idList = jdbcTemplate.queryForList(idPageSql, params.toArray());

            // SELECT ... FROM .?. WHERE id IN (...)
            params.clear();
            printSql.setLength(0);
            sql = QuerySqlUtil.toSelectWithIdSql(tcInfo, mainTable, fromSql, result, idList, needAlias, params, printSql);
        } else {
            // SELECT ... FROM ... WHERE ... ORDER BY ... limit ...
            sql = QuerySqlUtil.toPageSql(tcInfo, fromAndWhere, fromAndWherePrint, mainTable, param, result, needAlias, params, printSql);
        }
        return assemblyResult(sql, needAlias, params, mainTable, result, printSql);
    }

    private List<Map<String, Object>> queryPageListWithGroup(String selectGroupSql, String mainTable,
                                                             boolean needAlias, ReqParam param, ReqResult result,
                                                             List<Object> params, StringBuilder printSql) {
        String orderSql = param.generateOrderSql(mainTable, needAlias, tcInfo);
        printSql.append(orderSql);
        String sql = selectGroupSql + orderSql + param.generatePageSql(params, printSql);
        return assemblyResult(sql, needAlias, params, mainTable, result, printSql);
    }

    private List<Map<String, Object>> queryNoCountPage(String fromAndWhere, String fromAndWherePrint, String mainTable,
                                                       ReqParam param, ReqResult result, boolean needAlias, List<Object> params) {
        StringBuilder printSql = new StringBuilder();
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(tcInfo, fromAndWhere,
                fromAndWherePrint, mainTable, result, needAlias, params, printSql);
        String orderSql = param.generateOrderSql(mainTable, needAlias, tcInfo);
        printSql.append(orderSql);
        String sql = selectGroupSql + orderSql + param.generatePageSql(params, printSql);
        return assemblyResult(sql, needAlias, params, mainTable, result, printSql);
    }

    private List<Map<String, Object>> queryList(String fromAndWhere, String fromAndWherePrint, String mainTable,
                                                ReqParam param, ReqResult result, boolean needAlias, List<Object> params) {
        StringBuilder printSql = new StringBuilder();
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(tcInfo, fromAndWhere,
                fromAndWherePrint, mainTable, result, needAlias, params, printSql);
        String orderSql = param.generateOrderSql(mainTable, needAlias, tcInfo);
        String sql = selectGroupSql + orderSql;
        return assemblyResult(sql, needAlias, params, mainTable, result, printSql);
    }

    private Map<String, Object> queryObj(String fromAndWhere, String fromAndWherePrint, String mainTable,
                                         ReqParam param, ReqResult result, boolean needAlias, List<Object> params) {
        StringBuilder printSql = new StringBuilder();
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(tcInfo, fromAndWhere, fromAndWherePrint,
                mainTable, result, needAlias, params, printSql);
        String orderSql = param.generateOrderSql(mainTable, needAlias, tcInfo);
        String sql = selectGroupSql + orderSql + param.generateArrToObjSql(params, printSql);
        Map<String, Object> obj = QueryUtil.first(assemblyResult(sql, needAlias, params, mainTable, result, printSql));
        return QueryUtil.isNull(obj) ? Collections.emptyMap() : obj;
    }

    private List<Map<String, Object>> assemblyResult(String mainSql, boolean needAlias, List<Object> params,
                                                     String mainTable, ReqResult result, StringBuilder printSql) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("sql: [{}]", printSql);
        }
        List<Map<String, Object>> dataList = jdbcTemplate.queryForList(mainSql, params.toArray());
        if (QueryUtil.isNotEmpty(dataList)) {
            String mainTableName = tcInfo.findTable(mainTable).getName();
            handleData(dataList, needAlias, mainTableName, result);

            Set<String> removeColumn = result.needRemoveColumn(mainTableName, tcInfo, needAlias);
            for (Map<String, Object> data : dataList) {
                removeColumn.forEach(data::remove);
            }
        }
        return dataList;
    }

    private void handleData(List<Map<String, Object>> dataList, boolean needAlias, String mainTableName, ReqResult result) {
        for (Map<String, Object> data : dataList) {
            result.handleData(mainTableName, needAlias, data, tcInfo);
        }
        // order_address.order_id : order.id    +    order_item.code : order.code
        Map<String, ReqResult> innerResultMap = result.innerResult();
        if (QueryUtil.isNotEmpty(innerResultMap)) {
            // { address : id, items : code }
            Map<String, String> innerColumnMap = new LinkedHashMap<>();
            //  { address : { id1 : { ... },  id2 : { ... } }, items : { code1 : [ ... ], code2 : [ ... ] } }
            Map<String, Map<String, Object>> innerDataMap = new HashMap<>();
            for (Map.Entry<String, ReqResult> entry : innerResultMap.entrySet()) {
                String fieldName = entry.getKey();
                // { id : { id1 : { ... },  id2 : { ... } } }    or    { code : { code1 : [ ... ], code2 : [ ... ] } }
                Map<String, Map<String, Object>> valueMap = queryInnerData(mainTableName, entry.getValue(), dataList);
                if (QueryUtil.isNotEmpty(valueMap)) {
                    for (Map.Entry<String, Map<String, Object>> valueEntry : valueMap.entrySet()) {
                        innerColumnMap.put(fieldName, valueEntry.getKey());
                        innerDataMap.put(fieldName, valueEntry.getValue());
                    }
                }
            }
            for (Map<String, Object> data : dataList) {
                for (Map.Entry<String, String> entry : innerColumnMap.entrySet()) {
                    // address    or    items
                    String fieldName = entry.getKey();
                    // id    or    code
                    String columnName = entry.getValue();
                    // id1, id2    or    code1, code2
                    Object relationValue = data.get(columnName);
                    if (QueryUtil.isNotNull(relationValue)) {
                        // { id1 : { ... },  id2 : { ... } }    or    { code1 : [ ... ], code2 : [ ... ] }
                        Map<String, Object> innerData = innerDataMap.get(fieldName);
                        // put --> address : { ... }    or    items : [ ... ]
                        data.put(fieldName, innerData.get(QueryUtil.toStr(relationValue)));
                    }
                }
            }
        }
    }

    private Map<String, Map<String, Object>> queryInnerData(String tableName, ReqResult result,
                                                            List<Map<String, Object>> dataList) {
        String innerTable = result.getTable();
        // master-child
        boolean masterChild = true;
        TableColumnRelation relation = tcInfo.findRelationByMasterChild(tableName, innerTable);
        if (QueryUtil.isNull(relation)) {
            // child-master
            masterChild = false;
            relation = tcInfo.findRelationByMasterChild(innerTable, tableName);
        }
        if (QueryUtil.isNull(relation)) {
            return Collections.emptyMap();
        }

        TableColumn tableColumn = tcInfo.findTableColumn(relation.getOneTable(), relation.getOneColumn());
        String tableColumnAlias = tableColumn.getAlias();
        List<Object> relationIds = new ArrayList<>();
        for (Map<String, Object> data : dataList) {
            relationIds.add(data.get(tableColumnAlias));
        }
        if (QueryUtil.isEmpty(relationIds)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("data({}) no column({}) info", dataList, tableColumnAlias);
            }
            return Collections.emptyMap();
        }

        List<Map<String, Object>> mapList = new ArrayList<>();
        String relationColumn = QuerySqlUtil.toSqlField(tableColumn.getName());
        Set<String> removeColumn = new HashSet<>();
        String selectColumn = result.generateInnerSelect(relationColumn, tcInfo, removeColumn);
        String table = QuerySqlUtil.toSqlField(tcInfo.findTable(innerTable).getName());
        for (List<Object> ids : QueryUtil.split(relationIds, maxListCount)) {
            StringBuilder printSql = new StringBuilder();
            List<Object> params = new ArrayList<>();
            String innerSql = QuerySqlUtil.toInnerSql(selectColumn, table, relationColumn, ids, params, printSql);
            if (LOG.isDebugEnabled()) {
                LOG.debug("query inner sql: [{}]", printSql);
            }
            List<Map<String, Object>> idList = jdbcTemplate.queryForList(innerSql, params.toArray());
            if (QueryUtil.isNotEmpty(idList)) {
                mapList.addAll(idList);
            }
        }
        if (QueryUtil.isEmpty(mapList)) {
            return Collections.emptyMap();
        }

        handleData(mapList, false, innerTable, result);
        // { id1 : { ... },  id2 : { ... } }    or    { code1 : [ ... ], code2 : [ ... ] }
        Map<String, Object> innerDataMap = new HashMap<>();
        boolean hasMany = masterChild && relation.getType().hasMany();
        for (Map<String, Object> data : mapList) {
            if (QueryUtil.isNotNull(data)) {
                String key = QueryUtil.toStr(data.get(tableColumn.getAlias()));
                if (QueryUtil.isNotEmpty(key)) {
                    Object obj = innerDataMap.get(key);
                    removeColumn.forEach(data::remove);
                    if (hasMany) {
                        List<Object> list;
                        if (QueryUtil.isNotNull(obj) && (obj instanceof List<?>)) {
                            // noinspection unchecked
                            list = (List<Object>) obj;
                        } else {
                            list = new ArrayList<>();
                        }
                        list.add(data);
                        innerDataMap.put(key, list);
                    } else {
                        if (QueryUtil.isNotNull(obj)) {
                            switch (oneToOneHasMany) {
                                case 1: { break; }
                                case 2: {
                                    innerDataMap.put(key, data);
                                    break;
                                }
                                default: {
                                    throw new RuntimeException(String.format("%s, but multiple data(%s.%s - %s.%s : %s)",
                                            (masterChild ? "one-to-one" : "child-to-master"),
                                            relation.getOneTable(), relation.getOneColumn(),
                                            relation.getOneOrManyTable(), relation.getOneOrManyColumn(),
                                            key));
                                }
                            }
                        } else {
                            innerDataMap.put(key, data);
                        }
                    }
                }
            }
        }
        if (QueryUtil.isEmpty(innerDataMap)) {
            return Collections.emptyMap();
        }

        // { id : { id1 : { ... },  id2 : { ... } } }    or    { code : { code1 : [ ... ], code2 : [ ... ] } }
        Map<String, Map<String, Object>> returnMap = new HashMap<>();
        returnMap.put(tableColumn.getAlias(), innerDataMap);
        return returnMap;
    }
}
