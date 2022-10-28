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
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;

@SuppressWarnings({"unused", "DuplicatedCode"})
@Component
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

    @Value("${query.one-to-one-has-many-exception:false}")
    private boolean oneToOneHasManyException;

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
    public TableColumnTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterPropertiesSet() {
        if (QueryUtil.isNotEmpty(scanPackages)) {
            tcInfo = QueryInfoUtil.infoWithScan(tablePrefix, scanPackages,
                    logicDeleteColumn, logicValue, logicDeleteBooleanValue, logicDeleteIntValue, logicDeleteLongValue);
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
            tcInfo = QueryInfoUtil.infoWithDb(tablePrefix, tableList, tableColumnList, relationColumnList, indexList,
                    logicDeleteColumn, logicValue, logicDeleteBooleanValue, logicDeleteIntValue, logicDeleteLongValue);
        }
    }


    public TableColumnInfo getTcInfo() {
        return tcInfo;
    }


    public List<QueryInfo> info(String tables) {
        if (online) {
            return Collections.emptyList();
        }

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
                    String type = sc.getFieldType().getSimpleName();
                    Integer length = sc.getStrLen();
                    TableColumnRelation relation = tcInfo.findRelationByChild(table.getName(), sc.getName());
                    String relationTable, relationColumn;
                    if (QueryUtil.isNull(relation)) {
                        relationTable = null;
                        relationColumn = null;
                    } else {
                        Table tb = tcInfo.findTable(relation.getOneTable());
                        TableColumn tc = tb.getColumnMap().get(relation.getOneColumn());
                        relationTable = tb.getAlias();
                        relationColumn = tc.getAlias();
                    }
                    columnList.add(new QueryInfo.QueryColumn(sc.getAlias(), sc.getDesc(), type, length, relationTable, relationColumn));
                }
                queryList.add(new QueryInfo(table.getAlias(), table.getDesc(), columnList));
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
            if (LOG.isWarnEnabled()) {
                LOG.warn("insert: has no table({}) defined", table);
            }
            return 0;
        }

        List<Object> params = new ArrayList<>();
        String insertSql = tableInfo.generateInsertMap(data, generateNullField, params);
        if (QueryUtil.isEmpty(insertSql)) {
            return 0;
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
            if (LOG.isWarnEnabled()) {
                LOG.warn("insert: class({}) has no table defined", clazz.getName());
            }
            return 0;
        }

        List<Object> params = new ArrayList<>();
        String insertSql = table.generateInsert(obj, generateNullField, params);
        if (QueryUtil.isEmpty(insertSql)) {
            return 0;
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
            if (LOG.isWarnEnabled()) {
                LOG.warn("insert: class({}) has no table defined", clazz.getName());
            }
            return 0;
        }

        int flag = 0;
        for (List<T> lt : QueryUtil.split(list, singleCount)) {
            List<Object> params = new ArrayList<>();
            String batchInsertSql = table.generateBatchInsert(lt, generateNullField, params);
            if (QueryUtil.isNotEmpty(batchInsertSql)) {
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
        List<Object> params = new ArrayList<>();
        String deleteSql = table.generateDelete(query, tcInfo, params, force);
        if (QueryUtil.isEmpty(deleteSql)) {
            return 0;
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
                LOG.warn("update: has no table({}) defined", table);
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
                LOG.warn("update: has no table({}) defined", table);
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
                LOG.warn("update: has no table({}) defined", table);
            }
            return 0;
        }

        List<Object> params = new ArrayList<>();
        String updateSql = tableInfo.generateUpdateMap(updateObj, generateNullField, query, tcInfo, params);
        if (QueryUtil.isEmpty(updateSql)) {
            return 0;
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

        List<Object> params = new ArrayList<>();
        String updateSql = table.generateUpdate(updateObj, generateNullField, query, tcInfo, params);
        if (QueryUtil.isEmpty(updateSql)) {
            return 0;
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

        List<Object> params = new ArrayList<>();
        SingleTableWhere query = SingleTableWhere.buildId(tableInfo.idWhere(false), id);
        String querySql = tableInfo.generateQuery(query, tcInfo, params, tableInfo.generateSelect(true),
                null, null, null, QueryConst.LIMIT_ONE, force);
        if (QueryUtil.isEmpty(querySql)) {
            return Collections.emptyMap();
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
            List<Object> params = new ArrayList<>();
            SingleTableWhere query = SingleTableWhere.buildIds(idField, lt);
            String querySql = tableInfo.generateQuery(query, tcInfo, params, select,
                    null, null, null, null, force);
            if (QueryUtil.isNotEmpty(querySql)) {
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
        return query(table, query, null, null, null, null, true);
    }
    public List<Map<String, Object>> query(String table, SingleTableWhere query) {
        return query(table, query, null, null, null, null, false);
    }
    public List<Map<String, Object>> query(String table, SingleTableWhere query, String groupBy, String having,
                                           String orderBy, List<Integer> pageList, boolean force) {
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

        List<Object> params = new ArrayList<>();
        String querySql = tableInfo.generateQuery(query, tcInfo, params, tableInfo.generateSelect(false),
                groupBy, having, orderBy, pageList, force);
        if (QueryUtil.isEmpty(querySql)) {
            return Collections.emptyList();
        }
        return jdbcTemplate.queryForList(querySql, params.toArray());
    }
    public Map<String, Object> forceQueryOne(String table, SingleTableWhere query) {
        return QueryUtil.first(query(table, query, null, null, null, QueryConst.LIMIT_ONE, true));
    }
    public Map<String, Object> queryOne(String table, SingleTableWhere query) {
        return QueryUtil.first(query(table, query, null, null, null, QueryConst.LIMIT_ONE, false));
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

        List<Object> params = new ArrayList<>();
        String querySql = tableInfo.generateCountQuery(query, tcInfo, params, null, null, null, null, force);
        if (QueryUtil.isEmpty(querySql)) {
            return 0;
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

        List<Object> params = new ArrayList<>();
        SingleTableWhere query = SingleTableWhere.buildId(table.idWhere(false), id);
        String querySql = table.generateQuery(query, tcInfo, params, table.generateSelect(false),
                null, null, null, QueryConst.LIMIT_ONE, force);
        if (QueryUtil.isEmpty(querySql)) {
            return null;
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

        List<Object> params = new ArrayList<>();
        SingleTableWhere query = SingleTableWhere.buildIds(table.idWhere(false), ids);
        String querySql = table.generateQuery(query, tcInfo, params, table.generateSelect(false),
                null, null, null, null, force);
        if (QueryUtil.isEmpty(querySql)) {
            return Collections.emptyList();
        }
        return jdbcTemplate.queryForList(querySql, clazz, params.toArray());
    }
    public <T> List<T> queryByIds(Class<T> clazz, List<Serializable> ids) {
        return queryByIds(clazz, ids, false);
    }

    public <T> List<T> forceQuery(Class<T> clazz, SingleTableWhere query) {
        return query(clazz, query, null, null, null, null, true);
    }
    public <T> List<T> query(Class<T> clazz, SingleTableWhere query) {
        return query(clazz, query, null, null, null, null, false);
    }
    public <T> List<T> query(Class<T> clazz, SingleTableWhere query, String groupBy, String having,
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

        List<Object> params = new ArrayList<>();
        String querySql = table.generateQuery(query, tcInfo, params, table.generateSelect(false),
                groupBy, having, orderBy, pageList, force);
        if (QueryUtil.isEmpty(querySql)) {
            return Collections.emptyList();
        }
        return jdbcTemplate.queryForList(querySql, clazz, params.toArray());
    }
    public <T> T forceQueryOne(Class<T> clazz, SingleTableWhere query) {
        return QueryUtil.first(query(clazz, query, null, null, null, QueryConst.LIMIT_ONE, true));
    }
    public <T> T queryOne(Class<T> clazz, SingleTableWhere query) {
        return QueryUtil.first(query(clazz, query, null, null, null, QueryConst.LIMIT_ONE, false));
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

        List<Object> params = new ArrayList<>();
        String querySql = table.generateCountQuery(query, tcInfo, params, null, null, null, null, force);
        if (QueryUtil.isEmpty(querySql)) {
            return 0;
        }
        Long count = jdbcTemplate.queryForObject(querySql, Long.class, params.toArray());
        return QueryUtil.isNull(count) ? 0 : count;
    }
    public <T> long queryCount(Class<T> clazz, SingleTableWhere query) {
        return queryCount(clazz, query, false);
    }


    public Object dynamicQuery(RequestInfo req) {
        if (QueryUtil.isNull(req)) {
            return null;
        }

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
                return queryPage(firstFromSql, allFromSql, whereSql, mainTable, param, result, queryHasMany,
                        queryTableSet, allTableSet, params);
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
            // SELECT ... FROM ... WHERE .?. GROUP BY ... HAVING ..    (only where's table)
            String selectCountGroupSql = QuerySqlUtil.toSelectGroupSql(tcInfo, fromAndWhere, mainTable, result, queryTableSet, params);
            // SELECT COUNT(*) FROM ( ... ) tmp
            String countSql = QuerySqlUtil.toCountGroupSql(selectCountGroupSql);
            count = queryCount(countSql, params);
            if (param.needQueryCurrentPage(count)) {
                String fromAndWhereList = allFromSql + whereSql;
                // SELECT ... FROM ... WHERE .?. GROUP BY ... HAVING ... LIMIT ...    (all where's table)
                String selectListGroupSql = QuerySqlUtil.toSelectGroupSql(tcInfo, fromAndWhereList, mainTable, result, allTableSet, params);
                pageList = queryPageListWithGroup(selectListGroupSql, mainTable, !allTableSet.isEmpty(), param, result, params);
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
        return assemblyResult(sql, needAlias, params, mainTable, result);
    }

    private List<Map<String, Object>> queryPageListWithGroup(String selectGroupSql, String mainTable, boolean needAlias,
                                                             ReqParam param, ReqResult result, List<Object> params) {
        String sql = selectGroupSql + param.generatePageSql(params);
        return assemblyResult(sql, needAlias, params, mainTable, result);
    }

    private List<Map<String, Object>> queryList(String fromAndWhere, String mainTable, ReqParam param,
                                                ReqResult result, Set<String> allTableSet, List<Object> params) {
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(tcInfo, fromAndWhere, mainTable, result, allTableSet, params);
        boolean needAlias = !allTableSet.isEmpty();
        String orderSql = param.generateOrderSql(mainTable, needAlias, tcInfo);
        String sql = selectGroupSql + orderSql + param.generatePageSql(params);
        return assemblyResult(sql, needAlias, params, mainTable, result);
    }

    private List<Map<String, Object>> queryListNoLimit(String fromAndWhere, String mainTable, ReqParam param,
                                                       ReqResult result, Set<String> allTableSet, List<Object> params) {
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(tcInfo, fromAndWhere, mainTable, result, allTableSet, params);
        boolean needAlias = !allTableSet.isEmpty();
        String orderSql = param.generateOrderSql(mainTable, needAlias, tcInfo);
        String sql = selectGroupSql + orderSql;
        return assemblyResult(sql, needAlias, params, mainTable, result);
    }

    private Map<String, Object> queryObj(String fromAndWhere, String mainTable, ReqParam param, ReqResult result,
                                         Set<String> allTableSet, List<Object> params) {
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(tcInfo, fromAndWhere, mainTable, result, allTableSet, params);
        boolean needAlias = !allTableSet.isEmpty();
        String orderSql = param.generateOrderSql(mainTable, needAlias, tcInfo);
        String sql = selectGroupSql + orderSql + param.generateArrToObjSql(params);
        Map<String, Object> obj = QueryUtil.first(assemblyResult(sql, needAlias, params, mainTable, result));
        return QueryUtil.isNull(obj) ? Collections.emptyMap() : obj;
    }

    private List<Map<String, Object>> assemblyResult(String mainSql, boolean needAlias,
                                                     List<Object> params, String mainTable, ReqResult result) {
        List<Map<String, Object>> dataList = jdbcTemplate.queryForList(mainSql, params.toArray());
        if (QueryUtil.isNotEmpty(dataList)) {
            handleData(dataList, needAlias, tcInfo.findTable(mainTable).getName(), result);
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
            Map<String, String> innerColumnMap = new HashMap<>();
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

        String columnName = relation.getOneColumn();
        String tableColumnAlias = tcInfo.findTableColumn(relation.getOneTable(), columnName).getAlias();
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

        String relationColumn = QuerySqlUtil.toSqlField(columnName);
        String selectColumn = result.generateInnerSelect(relationColumn, tcInfo);
        String table = QuerySqlUtil.toSqlField(tcInfo.findTable(innerTable).getName());
        for (List<Object> ids : QueryUtil.split(relationIds, maxListCount)) {
            List<Object> params = new ArrayList<>();
            String innerSql = QuerySqlUtil.toInnerSql(selectColumn, table, relationColumn, ids, params);
            List<Map<String, Object>> idList = jdbcTemplate.queryForList(innerSql, params.toArray());
            if (QueryUtil.isNotEmpty(idList)) {
                mapList.addAll(idList);
            }
        }
        if (QueryUtil.isEmpty(mapList)) {
            return Collections.emptyMap();
        }

        handleData(mapList, false, tableName, result);
        // { id1 : { ... },  id2 : { ... } }    or    { code1 : [ ... ], code2 : [ ... ] }
        Map<String, Object> innerDataMap = new HashMap<>();
        boolean hasMany = masterChild && relation.getType().hasMany();
        for (Map<String, Object> data : mapList) {
            if (QueryUtil.isNotNull(data)) {
                String key = QueryUtil.toStr(data.get(columnName));
                if (QueryUtil.isNotEmpty(key)) {
                    Object obj = innerDataMap.get(key);
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
                            if (oneToOneHasManyException) {
                                throw new RuntimeException(String.format("%s, but multiple data(%s.%s - %s.%s : %s)",
                                        (masterChild ? "one-to-one" : "child to master"),
                                        relation.getOneTable(), relation.getOneColumn(),
                                        relation.getOneOrManyTable(), relation.getOneOrManyColumn(),
                                        key));
                            }
                        }
                        innerDataMap.put(key, data);
                    }
                }
            }
        }
        if (QueryUtil.isEmpty(innerDataMap)) {
            return Collections.emptyMap();
        }

        // { id : { id1 : { ... },  id2 : { ... } } }    or    { code : { code1 : [ ... ], code2 : [ ... ] } }
        Map<String, Map<String, Object>> returnMap = new HashMap<>();
        returnMap.put(columnName, innerDataMap);
        return returnMap;
    }
}
