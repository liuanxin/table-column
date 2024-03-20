package com.github.liuanxin.query.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.liuanxin.query.constant.QueryConst;
import com.github.liuanxin.query.enums.AliasGenerateRule;
import com.github.liuanxin.query.enums.OneToOneHasManyRule;
import com.github.liuanxin.query.enums.ResultType;
import com.github.liuanxin.query.enums.TableRelationType;
import com.github.liuanxin.query.model.*;
import com.github.liuanxin.query.util.QueryInfoUtil;
import com.github.liuanxin.query.util.QueryJsonUtil;
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

@SuppressWarnings("unchecked")
public class TableColumnTemplate implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(TableColumnTemplate.class);

    @Value("${query.scan-packages:}")
    private String scanPackages;

    @Value("${query.table-prefix:}")
    private String tablePrefix;

    @Value("${query.table-suffix:}")
    private String tableSuffix;

    @Value("${query.has-not-return-info:false}")
    private boolean hasNotReturnInfo;

    @Value("${query.required-alias:false}")
    private boolean requiredAlias;

    @Value("${query.not-required-condition-or-page:false}")
    private boolean notRequiredConditionOrPage;

    @Value("${query.alias-generate-rule:}")
    private AliasGenerateRule aliasGenerateRule;

    @Value("${query.deep-max-page-size:10000}")
    private int deepMaxPageSize;

    @Value("${query.max-list-count:1000}")
    private int maxListCount;

    @Value("${query.one-to-one-has-many-rule:}")
    private OneToOneHasManyRule oneToOneHasManyRule;

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

    private final List<TableColumnRelation> tableRelationList;
    private final Map<String, ReqAliasTemplate> queryAliasMap;
    private final JdbcTemplate jdbcTemplate;
    public TableColumnTemplate(JdbcTemplate jdbcTemplate,
                               List<TableColumnRelation> tableRelationList,
                               Map<String, ReqAliasTemplate> queryAliasMap) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableRelationList = QueryUtil.isEmpty(tableRelationList) ? new ArrayList<>() : new ArrayList<>(tableRelationList);
        this.queryAliasMap = queryAliasMap;
    }

    @Override
    public void afterPropertiesSet() {
        if (QueryUtil.isEmpty(scanPackages)) {
            loadDatabase();
        } else {
            tcInfo = QueryInfoUtil.infoWithScan(tablePrefix, tableSuffix, aliasGenerateRule, scanPackages, tableRelationList,
                    logicDeleteColumn, logicValue, logicDeleteBooleanValue, logicDeleteIntValue, logicDeleteLongValue);
            if (QueryUtil.isNull(tcInfo)) {
                throw new RuntimeException(String.format("class not found in(%s)", scanPackages));
            }
        }
        QueryInfoUtil.checkAndSetRelation(tableRelationList, tcInfo);
    }
    private void loadDatabase() {
        List<Map<String, Object>> tableList = new ArrayList<>();
        List<Map<String, Object>> tableColumnList = new ArrayList<>();
        loadDatabase(tableList, tableColumnList);
        tcInfo = QueryInfoUtil.infoWithDb(tablePrefix, aliasGenerateRule, tableList, tableColumnList,
                logicDeleteColumn, logicValue, logicDeleteBooleanValue, logicDeleteIntValue, logicDeleteLongValue);
    }
    private void loadDatabase(List<Map<String, Object>> tableList, List<Map<String, Object>> tableColumnList) {
        String dbName = jdbcTemplate.queryForObject(QueryConst.DB_SQL, String.class);
        // table_name, table_comment
        List<Map<String, Object>> tmpTableList = jdbcTemplate.queryForList(QueryConst.TABLE_SQL, dbName);
        if (QueryUtil.isNotEmpty(tmpTableList)) {
            tableList.addAll(tmpTableList);
        }
        // table_name, column_name, column_type, column_comment, has_pri, varchar_length
        List<Map<String, Object>> tmpColumnList = jdbcTemplate.queryForList(QueryConst.COLUMN_SQL, dbName);
        if (QueryUtil.isNotEmpty(tmpColumnList)) {
            tableColumnList.addAll(tmpColumnList);
        }
    }


    public TableColumnInfo getTcInfo() {
        return tcInfo;
    }


    public boolean refreshWithDatabase() {
        if (QueryUtil.isEmpty(scanPackages)) {
            loadDatabase();
            QueryInfoUtil.checkAndSetRelation(tableRelationList, tcInfo);
            return true;
        } else {
            return false;
        }
    }


    public void generateModel(String tables, String targetPath, String packagePath,
                              boolean generateComment, String modelPrefix, String modelSuffix) {
        List<Map<String, Object>> tableList = new ArrayList<>();
        List<Map<String, Object>> tableColumnList = new ArrayList<>();
        loadDatabase(tableList, tableColumnList);
        Set<String> tableSet = handleTable(tables);
        QueryInfoUtil.generateModel(tableSet, targetPath, packagePath, modelPrefix, modelSuffix,
                aliasGenerateRule, tablePrefix, generateComment, tableList, tableColumnList);
    }

    private Set<String> handleTable(String tables) {
        Set<String> tableSet = new LinkedHashSet<>();
        if (QueryUtil.isNotEmpty(tables)) {
            for (String te : tables.split(",")) {
                if (QueryUtil.isNotEmpty(te)) {
                    tableSet.add(te.trim().toLowerCase());
                }
            }
        }
        return tableSet;
    }


    public List<QueryInfo> forceInfo(String tables) {
        return info(tables, true);
    }
    public List<QueryInfo> info(String tables) {
        return info(tables, false);
    }
    /** @param force if table has logic field(with deleted), return filed info if this value is true */
    private List<QueryInfo> info(String tables, boolean force) {
        if (hasNotReturnInfo) {
            return Collections.emptyList();
        }

        Set<String> tableSet = handleTable(tables);
        List<QueryInfo> queryList = new ArrayList<>();
        for (Table table : tcInfo.allTable()) {
            String tableAlias = table.getAlias();
            if (QueryUtil.isEmpty(tableSet) || tableSet.contains(tableAlias.toLowerCase())) {
                List<QueryColumn> columnList = new ArrayList<>();
                for (TableColumn tc : table.getColumnMap().values()) {
                    String columnName = tc.getName();
                    if (!columnName.equals(table.getLogicColumn()) || force) {
                        String type = QueryConst.TYPE_MAP.get(tc.getFieldType());
                        Integer maxLength = tc.getStrLen();
                        TableColumnRelation relation = tcInfo.findRelationByChild(table.getName(), columnName);
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
                        columnList.add(new QueryColumn(tc.getAlias(), tc.getDesc(), type,
                                (needValue ? true : null), maxLength, relationTable, relationColumn));
                    }
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
        long start = System.currentTimeMillis();
        int flag = jdbcTemplate.update(insertSql, params.toArray());
        if (LOG.isDebugEnabled()) {
            LOG.debug("insert sql use time {} ms, change({})", (System.currentTimeMillis() - start), flag);
        }
        return flag;
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
                long start = System.currentTimeMillis();
                int update = jdbcTemplate.update(batchInsertSql, params.toArray());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("batch insert-map sql use time {} ms, change({})", (System.currentTimeMillis() - start), update);
                }
                flag += update;
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
        long start = System.currentTimeMillis();
        int flag = jdbcTemplate.update(insertSql, params.toArray());
        if (LOG.isDebugEnabled()) {
            LOG.debug("insert sql use time {} ms, change({})", (System.currentTimeMillis() - start), flag);
        }
        return flag;
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
                long start = System.currentTimeMillis();
                int update = jdbcTemplate.update(batchInsertSql, params.toArray());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("batch insert sql use time {} ms, change({})", (System.currentTimeMillis() - start), update);
                }
                flag += update;
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
        return doDelete(ReqQuery.buildId(tableInfo.idWhere(false), id), tableInfo, force);
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
        return doDelete(ReqQuery.buildIds(tableInfo.idWhere(false), ids), tableInfo, force);
    }
    @Transactional
    public int deleteByIds(String table, List<Serializable> ids) {
        return deleteByIds(table, ids, false);
    }

    @Transactional
    public int forceDelete(String table, ReqQuery query) {
        return delete(table, query, true);
    }
    private int delete(String table, ReqQuery query, boolean force) {
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
    public int delete(String table, ReqQuery query) {
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
        return doDelete(ReqQuery.buildId(table.idWhere(false), id), table, force);
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
        return doDelete(ReqQuery.buildIds(table.idWhere(false), ids), table, force);
    }
    @Transactional
    public <T> int deleteByIds(Class<T> clazz, List<Serializable> ids) {
        return deleteByIds(clazz, ids, false);
    }

    @Transactional
    public <T> int forceDelete(Class<T> clazz, ReqQuery query) {
        return delete(clazz, query, true);
    }
    private <T> int delete(Class<T> clazz, ReqQuery query, boolean force) {
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
    public <T> int delete(Class<T> clazz, ReqQuery query) {
        return delete(clazz, query, false);
    }

    private int doDelete(ReqQuery query, Table table, boolean force) {
        StringBuilder printSql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        String deleteSql = table.generateDelete(query, tcInfo, params, printSql, force);
        if (QueryUtil.isEmpty(deleteSql)) {
            return 0;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("delete sql: [{}]", printSql);
        }
        long start = System.currentTimeMillis();
        int flag = jdbcTemplate.update(deleteSql, params.toArray());
        if (LOG.isDebugEnabled()) {
            LOG.debug("delete sql use time {} ms, change({})", (System.currentTimeMillis() - start), flag);
        }
        return flag;
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
        return update(table, updateObj, ReqQuery.buildId(tableInfo.idWhere(false), id));
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
        return update(table, updateObj, ReqQuery.buildIds(tableInfo.idWhere(false), ids));
    }

    @Transactional
    public int update(String table, Map<String, Object> updateObj, ReqQuery query) {
        return update(table, updateObj, query, false);
    }

    @Transactional
    public int update(String table, Map<String, Object> updateObj, ReqQuery query, boolean generateNullField) {
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
        long start = System.currentTimeMillis();
        int flag = jdbcTemplate.update(updateSql, params.toArray());
        if (LOG.isDebugEnabled()) {
            LOG.debug("update-map sql use time {} ms, change({})", (System.currentTimeMillis() - start), flag);
        }
        return flag;
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
        return update(updateObj, ReqQuery.buildId(table.idWhere(false), id));
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
        return update(updateObj, ReqQuery.buildIds(table.idWhere(false), ids));
    }

    @Transactional
    public <T> int update(T updateObj, ReqQuery query) {
        return update(updateObj, query, false);
    }

    @Transactional
    public <T> int update(T updateObj, ReqQuery query, boolean generateNullField) {
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
        long start = System.currentTimeMillis();
        int flag = jdbcTemplate.update(updateSql, params.toArray());
        if (LOG.isDebugEnabled()) {
            LOG.debug("update sql use time {} ms, change({})", (System.currentTimeMillis() - start), flag);
        }
        return flag;
    }


    public long forceQueryCount(QueryData req) {
        return queryCount(req, true);
    }
    private long queryCount(QueryData req, boolean force) {
        Map<String, Object> data = QueryJsonUtil.convertData(dynamicQuery(req.toQueryCount(tcInfo), force));
        return QueryUtil.isEmpty(data) ? 0L : QueryUtil.toLong(data.get(QueryConst.COUNT_ALIAS));
    }
    private long queryCount(QueryData req) {
        return queryCount(req, false);
    }

    public <T> List<T> forceQuery(QueryData req, Class<T> clazz) {
        return query(req, clazz, true);
    }
    private <T> List<T> query(QueryData req, Class<T> clazz, boolean force) {
        return QueryJsonUtil.convertList(dynamicQuery(req.toQueryList(tcInfo), force), clazz);
    }
    public <T> List<T> query(QueryData req, Class<T> clazz) {
        return query(req, clazz, false);
    }

    public <T> T forceQueryOne(QueryData req, Class<T> clazz) {
        return queryOne(req, clazz, true);
    }
    private <T> T queryOne(QueryData req, Class<T> clazz, boolean force) {
        return QueryJsonUtil.convert(dynamicQuery(req.toQueryObj(tcInfo), force), clazz);
    }
    public <T> T queryOne(QueryData req, Class<T> clazz) {
        return queryOne(req, clazz, false);
    }

    public <T> PageReturn<T> forceQueryPage(QueryData req, Class<T> clazz) {
        return queryPage(req, clazz, true);
    }
    private <T> PageReturn<T> queryPage(QueryData req, Class<T> clazz, boolean force) {
        return QueryJsonUtil.convertType(dynamicQuery(req.toQueryPage(tcInfo), force), new TypeReference<PageReturn<T>>() {});
    }
    public <T> PageReturn<T> queryPage(QueryData req, Class<T> clazz) {
        return queryPage(req, clazz, false);
    }



    public List<Map<String, Object>> forceQuery(QueryData req) {
        return query(req, true);
    }
    private List<Map<String, Object>> query(QueryData req, boolean force) {
        return QueryJsonUtil.convertDateList(dynamicQuery(req.toQueryList(tcInfo), force));
    }
    public List<Map<String, Object>> query(QueryData req) {
        return query(req, false);
    }

    public Map<String, Object> forceQueryOne(QueryData req) {
        return queryOne(req, false);
    }
    private Map<String, Object> queryOne(QueryData req, boolean force) {
        return QueryJsonUtil.convertData(dynamicQuery(req.toQueryObj(tcInfo), force));
    }
    public Map<String, Object> queryOne(QueryData req) {
        return queryOne(req, true);
    }

    public PageReturn<Map<String, Object>> forceQueryPage(QueryData req) {
        return queryPage(req, true);
    }
    private PageReturn<Map<String, Object>> queryPage(QueryData req, boolean force) {
        return QueryJsonUtil.convertType(dynamicQuery(req.toQueryPage(tcInfo), force), new TypeReference<PageReturn<Map<String, Object>>>() {});
    }
    public PageReturn<Map<String, Object>> queryPage(QueryData req) {
        return queryPage(req, false);
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
                LOG.warn("query-id: has no table({}) defined", table);
            }
            return Collections.emptyMap();
        }

        ReqQuery query = ReqQuery.buildId(tableInfo.idWhere(false), id);
        ReqInfo req = new ReqInfo(tableInfo.getAlias(), new ReqParam(query), ResultType.OBJ, null);
        return QueryJsonUtil.convertData(dynamicQuery(req, force));
    }
    public Map<String, Object> queryById(String table, Serializable id) {
        return queryById(table, id, false);
    }

    public List<Map<String, Object>> forceQueryByIds(String table, List<Serializable> ids) {
        return queryByIds(table, ids, true);
    }
    private List<Map<String, Object>> queryByIds(String table, List<Serializable> ids, boolean force) {
        if (QueryUtil.isNotEmpty(ids) && ids.size() > maxListCount) {
            throw new RuntimeException(String.format("single query count max(%s) current(%s)", maxListCount, ids.size()));
        }
        if (QueryUtil.isEmpty(table) || QueryUtil.isIllegalIdList(ids)) {
            return Collections.emptyList();
        }

        Table tableInfo = tcInfo.findTable(table.trim());
        if (QueryUtil.isNull(tableInfo)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("query-ids: has no table({}) defined", table);
            }
            return Collections.emptyList();
        }

        ReqQuery query = ReqQuery.buildIds(tableInfo.idWhere(false), ids);
        ReqInfo req = new ReqInfo(tableInfo.getAlias(), new ReqParam(query), null, null);
        return QueryJsonUtil.convertDateList(dynamicQuery(req, force));
    }
    public List<Map<String, Object>> queryByIds(String table, List<Serializable> ids) {
        return queryByIds(table, ids, false);
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
                LOG.warn("query-id: class({}) has no table defined", clazz.getName());
            }
            return null;
        }

        ReqQuery query = ReqQuery.buildId(table.idWhere(false), id);
        ReqInfo req = new ReqInfo(table.getAlias(), new ReqParam(query), ResultType.OBJ, null);
        return QueryJsonUtil.convert(dynamicQuery(req, force), clazz);
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
                LOG.warn("query-ids: class({}) no table defined", clazz.getName());
            }
            return Collections.emptyList();
        }

        ReqQuery query = ReqQuery.buildIds(table.idWhere(false), ids);
        ReqInfo req = new ReqInfo(table.getAlias(), new ReqParam(query), null, null);
        return QueryJsonUtil.convertList(dynamicQuery(req, force), clazz);
    }
    public <T> List<T> queryByIds(Class<T> clazz, List<Serializable> ids) {
        return queryByIds(clazz, ids, false);
    }


    public long forceQueryCount(String table, ReqQuery query) {
        return queryCount(table, query, true);
    }
    private long queryCount(String table, ReqQuery query, boolean force) {
        if (QueryUtil.isEmpty(table) || QueryUtil.isNull(query)) {
            return 0;
        }

        Table tableInfo = tcInfo.findTable(table);
        if (QueryUtil.isNull(tableInfo)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("query-count: has no table({}) defined", table);
            }
            return 0;
        }

        return handleCount(tableInfo, query, force);
    }

    private long handleCount(Table table, ReqQuery query, boolean force) {
        ReqInfo req = new ReqInfo(table.getAlias(), new ReqParam(query), null, null);
        req.setResult(new ReqResult(Arrays.asList(QueryConst.COUNT_ALIAS, "count", "*")));
        Map<String, Object> data = QueryUtil.first(QueryJsonUtil.convertDateList(dynamicQuery(req, force)));
        return QueryUtil.isNotEmpty(data) ? QueryUtil.toLong(data.get(QueryConst.COUNT_ALIAS)) : 0;
    }

    public long queryCount(String table, ReqQuery query) {
        return queryCount(table, query, false);
    }

    public <T> long forceQueryCount(Class<T> clazz, ReqQuery query) {
        return queryCount(clazz, query, true);
    }
    public <T> long queryCount(Class<T> clazz, ReqQuery query) {
        return queryCount(clazz, query, false);
    }
    private <T> long queryCount(Class<T> clazz, ReqQuery query, boolean force) {
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

        return handleCount(table, query, force);
    }


    public Object forceDynamicQuery(String alias, ReqAlias req) {
        return dynamicQuery(alias, req, true);
    }
    public Object dynamicQuery(String alias, ReqAlias req) {
        return dynamicQuery(alias, req, false);
    }
    private Object dynamicQuery(String alias, ReqAlias req, boolean force) {
        if (QueryUtil.isEmpty(alias) || QueryUtil.isNull(req)) {
            return null;
        }

        ReqInfo reqInfo = req.handleAlias(alias, queryAliasMap);
        if (QueryUtil.isNull(reqInfo)) {
            if (LOG.isInfoEnabled()) {
                LOG.info("alias({} + {}) parse to null", alias, QueryJsonUtil.toJson(req));
            }
            return null;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("alias({} + {}) parse to ({})", alias, QueryJsonUtil.toJson(req), QueryJsonUtil.toJson(reqInfo));
        }
        return dynamicQuery(reqInfo, force);
    }

    public Object forceDynamicQuery(ReqInfo req) {
        checkAlias();
        return dynamicQuery(req, true);
    }
    public Object dynamicQuery(ReqInfo req) {
        checkAlias();
        return dynamicQuery(req, false);
    }
    private void checkAlias() {
        if (requiredAlias) {
            throw new RuntimeException("request: required request alias");
        }
        if (QueryUtil.isEmpty(queryAliasMap)) {
            throw new RuntimeException("request: no define alias info");
        }
    }
    private Object dynamicQuery(ReqInfo req, boolean force) {
        if (QueryUtil.isNull(req)) {
            return null;
        }

        req.checkTable(tcInfo);

        Set<String> paramTableSet = req.checkParam(notRequiredConditionOrPage, tcInfo, maxListCount);
        req.checkResult(tcInfo, force);
        Set<TableJoinRelation> useRelationSet = req.checkRelation(tcInfo, paramTableSet);
        Set<String> useTableSet = calcTableSet(useRelationSet);
        req.checkAllTable(tcInfo, useTableSet, paramTableSet);

        String mainTable = req.getTable();
        ReqParam param = req.getParam();
        ReqResult result = req.getResult();
        boolean needAlias = QueryUtil.isNotEmpty(useTableSet);

        String fromSql = QuerySqlUtil.toFromSql(tcInfo, mainTable, useRelationSet);
        List<Object> params = new ArrayList<>();
        StringBuilder wherePrintSbd = new StringBuilder();
        String whereSql = param.generateWhereSql(mainTable, tcInfo, needAlias, params, useTableSet, force, wherePrintSbd);
        String fromAndWhere = fromSql + whereSql;
        String wherePrint = wherePrintSbd.toString();
        String fromAndWherePrint = fromSql + wherePrint;
        boolean hasDistinct = queryHasDistinct(useRelationSet) && QueryUtil.toBool(result.isDistinct());

        /*
        plus sign indicate branch
        number indicate steps

        + query page (include count)
          + group by
            1. query count : SELECT COUNT(*) FROM ( SELECT ... FROM ... WHERE ... GROUP BY ... HAVING ... ) TMP
            2. query list : SELECT ... FROM ... WHERE ... GROUP BY ... HAVING ... ORDER BY ... LIMIT ...
          + no group by
            1. query count
              - multiple table: SELECT COUNT(DISTINCT main-table.id) FROM main-table xxxx join other-table on ...
              - single table:   SELECT COUNT(*) FROM main-table...
            2. query list
              + query deep page
                2.1. SELECT id FROM ... WHERE ... ORDER BY ... LIMIT x, x
                2.2. SELECT ... FROM ... WHERE id IN (...)
              + query no deep page
                > SELECT ... FROM ... WHERE ... ORDER BY ... LIMIT x, x

        + query page (exclusive count)
          > SELECT ... FROM ... WHERE ... GROUP BY ... HAVING ... ORDER BY ... LIMIT x, x

        + query one (no page)
          > SELECT ... FROM ... WHERE ... GROUP BY ... HAVING ... ORDER BY ... LIMIT 1

        + query list (no page)
          > SELECT ... FROM ... WHERE ... GROUP BY ... HAVING ... ORDER BY ...

        return data after assembly
        */

        if (param.needQueryPage()) {
            if (param.needQueryCount()) {
                return queryCountPage(fromSql, whereSql, wherePrint, mainTable, param, result, needAlias, hasDistinct, params, force);
            } else {
                return queryNoCountPage(fromAndWhere, fromAndWherePrint, mainTable, param, result, needAlias, hasDistinct, params, force);
            }
        } else {
            if (req.getType() == ResultType.OBJ) {
                return queryObj(fromAndWhere, fromAndWherePrint, mainTable, param, result, needAlias, hasDistinct, params, force);
            } else {
                return queryList(fromAndWhere, fromAndWherePrint, mainTable, param, result, needAlias, hasDistinct, params, force);
            }
        }
    }

    private Set<String> calcTableSet(Set<TableJoinRelation> relationSet) {
        Set<String> tableSet = new HashSet<>();
        if (QueryUtil.isNotEmpty(relationSet)) {
            for (TableJoinRelation joinRelation : relationSet) {
                tableSet.add(joinRelation.getMasterTable().getName());
                tableSet.add(joinRelation.getChildTable().getName());
            }
        }
        return tableSet;
    }
    private boolean queryHasDistinct(Set<TableJoinRelation> paramRelationSet) {
        if (QueryUtil.isNotEmpty(paramRelationSet)) {
            for (TableJoinRelation joinRelation : paramRelationSet) {
                String masterTableName = joinRelation.getMasterTable().getName();
                String childTableName = joinRelation.getChildTable().getName();
                TableColumnRelation relation = tcInfo.findRelationByMasterChild(masterTableName, childTableName);
                if (QueryUtil.isNotNull(relation) && relation.getType().hasMany()) {
                    return true;
                }
            }
        }
        return false;
    }

    private Map<String, Object> queryCountPage(String fromSql, String whereSql, String wherePrint, String mainTable,
                                               ReqParam param, ReqResult result, boolean needAlias,
                                               boolean hasDistinct, List<Object> params, boolean force) {
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
                    fromAndWherePrint, mainTable, result, needAlias, force, hasDistinct, params, printSql);
            // SELECT COUNT(*) FROM ( ^^^ SELECT FROM WHERE GROUP BY HAVING ^^^ ) tmp
            String selectGroupCountSql = QuerySqlUtil.toCountGroupSql(selectGroupSql, printSql.toString(), countPrintSql);
            count = queryCount(selectGroupCountSql, params, countPrintSql);
            if (param.needQueryCurrentPage(count)) {
                // ^^^ SELECT FROM WHERE GROUP BY HAVING ^^^ ORDER BY ... LIMIT ..
                pageList = queryPageListWithGroup(selectGroupSql, mainTable, needAlias, param, result, params, force, printSql);
            } else {
                pageList = Collections.emptyList();
            }
        } else {
            // SELECT COUNT(DISTINCT id) FROM ... WHERE ...
            String selectCountSql = QuerySqlUtil.toCountWithoutGroupSql(tcInfo, fromAndWhere,
                    fromAndWherePrint, mainTable, needAlias, hasDistinct, countPrintSql);
            count = queryCount(selectCountSql, params, countPrintSql);
            if (param.needQueryCurrentPage(count)) {
                pageList = queryLimitList(fromSql, whereSql, wherePrint, mainTable, param, result, needAlias,
                        hasDistinct, params, force, printSql);
            } else {
                pageList = Collections.emptyList();
            }
        }
        Map<String, Object> pageInfo = new LinkedHashMap<>();
        pageInfo.put("count", count);
        if (QueryUtil.isNotNull(pageList)) {
            pageInfo.put("list", pageList);
        }
        return pageInfo;
    }

    private long queryCount(String countSql, List<Object> params, StringBuilder printSql) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("query count sql: [{}]", printSql);
        }
        long start = System.currentTimeMillis();
        Long count = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        if (LOG.isDebugEnabled()) {
            LOG.debug("query count sql use time {} ms, count({})", (System.currentTimeMillis() - start), count);
        }
        return QueryUtil.isNull(count) ? 0L : count;
    }

    private List<Map<String, Object>> queryLimitList(String fromSql, String whereSql, String wherePrint, String mainTable,
                                                     ReqParam param, ReqResult result, boolean needAlias, boolean hasDistinct,
                                                     List<Object> params, boolean force, StringBuilder printSql) {
        String fromAndWhere = fromSql + whereSql;
        String fromAndWherePrint = fromSql + wherePrint;
        String sql;
        // deep paging(need offset a lot of result), use 「where + order + limit」 to query id, then use id to query specific columns
        if (param.hasDeepPage(deepMaxPageSize)) {
            // SELECT id FROM ... WHERE .?. ORDER BY ... LIMIT ...
            String idPageSql = QuerySqlUtil.toIdPageSql(tcInfo, fromAndWhere,
                    fromAndWherePrint, mainTable, needAlias, param, params, hasDistinct, printSql);
            if (LOG.isDebugEnabled()) {
                LOG.debug("query condition sql: [{}]", printSql);
            }
            long start = System.currentTimeMillis();
            List<Map<String, Object>> idList = jdbcTemplate.queryForList(idPageSql, params.toArray());
            if (LOG.isDebugEnabled()) {
                LOG.debug("query condition sql use time {} ms, size({})", (System.currentTimeMillis() - start), idList.size());
            }

            // SELECT ... FROM .?. WHERE id IN (...)
            params.clear();
            printSql.setLength(0);
            sql = QuerySqlUtil.toSelectWithIdSql(tcInfo, mainTable, fromSql, result, idList, needAlias, force, params, printSql);
        } else {
            // SELECT ... FROM ... WHERE ... ORDER BY ... limit ...
            sql = QuerySqlUtil.toPageSql(tcInfo, fromAndWhere, fromAndWherePrint, mainTable,
                    param, result, needAlias, force, params, hasDistinct, printSql);
        }
        return QueryUtil.isEmpty(sql) ? null : assemblyResult(sql, needAlias, params, mainTable, result, force, printSql);
    }

    private List<Map<String, Object>> queryPageListWithGroup(String selectGroupSql, String mainTable,
                                                             boolean needAlias, ReqParam param, ReqResult result,
                                                             List<Object> params, boolean force, StringBuilder printSql) {
        String orderSql = param.generateOrderSql(mainTable, needAlias, tcInfo);
        printSql.append(orderSql);
        String sql = selectGroupSql + orderSql + param.generatePageSql(params, printSql);
        return assemblyResult(sql, needAlias, params, mainTable, result, force, printSql);
    }

    private List<Map<String, Object>> queryNoCountPage(String fromAndWhere, String fromAndWherePrint, String mainTable,
                                                       ReqParam param, ReqResult result, boolean needAlias,
                                                       boolean hasDistinct, List<Object> params, boolean force) {
        StringBuilder printSql = new StringBuilder();
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(tcInfo, fromAndWhere,
                fromAndWherePrint, mainTable, result, needAlias, force, hasDistinct, params, printSql);
        String orderSql = param.generateOrderSql(mainTable, needAlias, tcInfo);
        printSql.append(orderSql);
        String sql = selectGroupSql + orderSql + param.generatePageSql(params, printSql);
        return assemblyResult(sql, needAlias, params, mainTable, result, force, printSql);
    }

    private List<Map<String, Object>> queryList(String fromAndWhere, String fromAndWherePrint, String mainTable,
                                                ReqParam param, ReqResult result, boolean needAlias,
                                                boolean hasDistinct, List<Object> params, boolean force) {
        StringBuilder printSql = new StringBuilder();
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(tcInfo, fromAndWhere,
                fromAndWherePrint, mainTable, result, needAlias, force, hasDistinct, params, printSql);
        String orderSql = param.generateOrderSql(mainTable, needAlias, tcInfo);
        String sql = selectGroupSql + orderSql;
        return assemblyResult(sql, needAlias, params, mainTable, result, force, printSql);
    }

    private Map<String, Object> queryObj(String fromAndWhere, String fromAndWherePrint, String mainTable,
                                         ReqParam param, ReqResult result, boolean needAlias,
                                         boolean hasDistinct, List<Object> params, boolean force) {
        StringBuilder printSql = new StringBuilder();
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(tcInfo, fromAndWhere, fromAndWherePrint,
                mainTable, result, needAlias, force, hasDistinct, params, printSql);
        String orderSql = param.generateOrderSql(mainTable, needAlias, tcInfo);
        String sql = selectGroupSql + orderSql + param.generateArrToObjSql(params, printSql);
        Map<String, Object> obj = QueryUtil.first(assemblyResult(sql, needAlias, params, mainTable, result, force, printSql));
        return QueryUtil.isNull(obj) ? Collections.emptyMap() : obj;
    }

    private List<Map<String, Object>> assemblyResult(String mainSql, boolean needAlias, List<Object> params,
                                                     String mainTable, ReqResult result, boolean force,
                                                     StringBuilder printSql) {
        if (LOG.isInfoEnabled()) {
            LOG.info("sql: [{}]", printSql);
        }
        long start = System.currentTimeMillis();
        List<Map<String, Object>> dataList = jdbcTemplate.queryForList(mainSql, params.toArray());
        if (LOG.isDebugEnabled()) {
            LOG.debug("sql use time {} ms, size({})", (System.currentTimeMillis() - start), dataList.size());
        }
        if (QueryUtil.isNotEmpty(dataList)) {
            String mainTableName = tcInfo.findTable(mainTable).getName();
            handleData(dataList, needAlias, mainTableName, result, force);

            Set<String> removeColumn = result.needRemoveColumn(mainTableName, tcInfo, needAlias, force);
            for (Map<String, Object> data : dataList) {
                removeColumn.forEach(data::remove);
            }
        }
        return dataList;
    }

    private void handleData(List<Map<String, Object>> dataList, boolean needAlias,
                            String mainTableName, ReqResult result, boolean force) {
        for (Map<String, Object> data : dataList) {
            result.handleData(mainTableName, needAlias, data, tcInfo, force);
        }
        // order_address.order_id : order.id    +    order_item.code : order.code
        Map<String, ReqResult> innerResultMap = result.innerResult(tcInfo, force);
        if (QueryUtil.isNotEmpty(innerResultMap)) {
            // { address : id, items : code }
            Map<String, String> innerColumnMap = new LinkedHashMap<>();
            //  { address : { id1 : { ... },  id2 : { ... } }, items : { code1 : [ ... ], code2 : [ ... ] } }
            Map<String, Map<String, Object>> innerDataMap = new HashMap<>();
            for (Map.Entry<String, ReqResult> entry : innerResultMap.entrySet()) {
                String fieldName = entry.getKey();
                // { id : { id1 : { ... },  id2 : { ... } } }    or    { code : { code1 : [ ... ], code2 : [ ... ] } }
                Map<String, Map<String, Object>> valueMap = queryInnerData(mainTableName, entry.getValue(), force, dataList);
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
                        Object value = innerData.get(QueryUtil.toStr(relationValue));
                        if (QueryUtil.isNotNull(value)) {
                            data.put(fieldName, value);
                        }
                    }
                }
            }
        }
    }

    private Map<String, Map<String, Object>> queryInnerData(String tableName, ReqResult result, boolean force,
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

        boolean needAlias = false;
        List<Map<String, Object>> mapList = new ArrayList<>();
        String relationColumn = QuerySqlUtil.toSqlField(tableColumn.getName());
        Set<String> removeColumn = new HashSet<>();
        String selectColumn = result.generateInnerSelect(relationColumn, tcInfo, removeColumn, force);
        Table tableInfo = tcInfo.findTable(innerTable);
        String table = QuerySqlUtil.toSqlField(tableInfo.getName());
        String logicDelete = tableInfo.logicDeleteCondition(force, needAlias);
        List<Object> params = new ArrayList<>();
        for (List<Object> ids : QueryUtil.split(relationIds, maxListCount)) {
            params.clear();
            StringBuilder printSql = new StringBuilder();
            String innerSql = QuerySqlUtil.toInnerSql(selectColumn, table, relationColumn, ids, params, printSql, logicDelete);
            if (LOG.isDebugEnabled()) {
                LOG.debug("query inner sql: [{}]", printSql);
            }
            long start = System.currentTimeMillis();
            List<Map<String, Object>> idList = jdbcTemplate.queryForList(innerSql, params.toArray());
            if (LOG.isDebugEnabled()) {
                LOG.debug("query inner sql use time {} ms, size({})", (System.currentTimeMillis() - start), idList.size());
            }
            if (QueryUtil.isNotEmpty(idList)) {
                mapList.addAll(idList);
            }
        }
        if (QueryUtil.isEmpty(mapList)) {
            return Collections.emptyMap();
        }

        handleData(mapList, needAlias, innerTable, result, force);
        // { id1 : { ... },  id2 : { ... } }    or    { code1 : [ ... ], code2 : [ ... ] }
        Map<String, Object> innerDataMap = new HashMap<>();
        TableRelationType relationType = relation.getType();
        // array : ( (master -> child) && (relation not one-to-one) )  ||  ( (child -> master) && (relation has many-to-many) )
        boolean hasMany = (masterChild && relationType.hasMany()) || (!masterChild && relationType.hasManyMaster());
        for (Map<String, Object> data : mapList) {
            if (QueryUtil.isNotNull(data)) {
                String key = QueryUtil.toStr(data.get(tableColumn.getAlias()));
                if (QueryUtil.isNotEmpty(key)) {
                    Object obj = innerDataMap.get(key);
                    removeColumn.forEach(data::remove);
                    if (hasMany) {
                        List<Object> list;
                        if (QueryUtil.isNotNull(obj) && (obj instanceof List<?>)) {
                            list = (List<Object>) obj;
                        } else {
                            list = new ArrayList<>();
                        }
                        list.add(data);
                        innerDataMap.put(key, list);
                    } else {
                        if (QueryUtil.isNotNull(obj)) {
                            switch (QueryUtil.defaultIfNull(oneToOneHasManyRule, OneToOneHasManyRule.Exception)) {
                                case First: { break; }
                                case Cover: {
                                    innerDataMap.put(key, data);
                                    break;
                                }
                                default: {
                                    throw new RuntimeException(String.format("%s, but data has multi(%s.%s - %s.%s : %s)",
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
