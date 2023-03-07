package com.github.liuanxin.query.model;

import com.github.liuanxin.query.constant.QueryConst;
import com.github.liuanxin.query.enums.OperateType;
import com.github.liuanxin.query.util.QueryJsonUtil;
import com.github.liuanxin.query.util.QueryUtil;

import java.io.Serializable;
import java.util.*;

/**
 * <pre>
 * name like '...%'
 * and time >= '...'
 * and time <= '...'
 * and ( gender = ... or age between ... and ... )
 * and ( province in ( ... ) or city like '%...%' )
 * and status = ...
 *
 *
 * {
 *   "operate": "and",
 *   "conditions": [
 *     { "name": "$start" },
 *     { "_meta_name_": "startTime", "time": "$ge" },
 *     { "_meta_name_": "endTime", "time": "$le" },
 *     {
 *       "operate": "or",
 *       "name": "x",
 *       "conditions": [
 *         { "gender": "$eq" },
 *         { "age": "$bet" }
 *       ]
 *     },
 *     {
 *       "operate": "or",
 *       "name": "y",
 *       "conditions": [
 *         { "province": "$in" },
 *         { "city": "$fuzzy" }
 *       ]
 *     },
 *     { "status": "$eq" }
 *   ]
 * }
 * </pre>
 */
public class ReqAliasTemplateQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    private OperateType operate;
    private String name;
    private List<Object> conditions;

    public ReqAliasTemplateQuery() {}
    public ReqAliasTemplateQuery(OperateType operate, List<Object> conditions) {
        this.operate = operate;
        this.conditions = conditions;
    }
    public ReqAliasTemplateQuery(OperateType operate, String name, List<Object> conditions) {
        this.operate = operate;
        this.name = name;
        this.conditions = conditions;
    }

    public OperateType getOperate() {
        return operate;
    }
    public void setOperate(OperateType operate) {
        this.operate = operate;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public List<Object> getConditions() {
        return conditions;
    }
    public void setConditions(List<Object> conditions) {
        this.conditions = conditions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReqAliasTemplateQuery that = (ReqAliasTemplateQuery) o;
        return operate == that.operate && Objects.equals(name, that.name) && Objects.equals(conditions, that.conditions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operate, name, conditions);
    }

    @Override
    public String toString() {
        return "ReqAliasQuery{" +
                "operate=" + operate +
                ", name='" + name + '\'' +
                ", conditions=" + conditions +
                '}';
    }


    /**
     * <pre>
     * 模板
     * {
     *   "operate": "and",
     *   "conditions": [
     *     { "name": "$start" },
     *     { "_meta_name_": "startTime", "time": "$ge" },
     *     { "_meta_name_": "endTime", "time": "$le" },
     *     {
     *       "operate": "or",
     *       "name": "x",
     *       "conditions": [
     *         { "gender": "$eq" },
     *         { "age": "$bet" }
     *       ]
     *     },
     *     {
     *       "operate": "or",
     *       "name": "y",
     *       "conditions": [
     *         { "province": "$in" },
     *         { "city": "$fuzzy" }
     *       ]
     *     },
     *     { "status": "$eq" }
     *   ]
     * }
     *
     * 数据
     * {
     *   "name": "abc",
     *   "startTime": "xxxx-xx-xx xx:xx:xx",
     *   "endTime": "yyyy-yy-yy yy:yy:yy",
     *   "x": { "gender": 1, "age": [ 18, 40 ] },
     *   "y": { "province": [ "x", "y", "z" ], "city": "xx" },
     *   "status": 1
     * }
     *
     *
     * 最终生成
     * {
     *   "operate": "and",
     *   "conditions": [
     *     [ "name", "$start", "abc" ],
     *     [ "time", "$ge", "xxxx-xx-xx xx:xx:xx" ],
     *     [ "time", "$le", "yyyy-yy-yy yy:yy:yy" ],
     *     {
     *       "operate": "or",
     *       "conditions": [
     *         [ "gender", "$eq", 1 ],
     *         [ "age", "$bet", [ 18, 40 ] ]
     *       ]
     *     },
     *     {
     *       "operate": "or",
     *       "conditions": [
     *         [ "province", "$in", [ "x", "y", "z" ] ],
     *         [ "city", "$fuzzy", "xx" ]
     *       ]
     *     },
     *     [ "status", "$eq", 1 ]
     *   ]
     * }
     *
     * 对应的查询是
     * name like 'abc%'
     * and time >= 'xxxx-xx-xx xx:xx:xx'
     * and time <= 'yyyy-yy-yy yy:yy:yy'
     * and ( gender = 1 or age between 18 and 40 )
     * and ( province in ( 'x', 'y', 'z' ) or city like '%xx%' )
     * and status = 1
     * </pre>
     */
    public ReqQuery transfer(Map<String, Object> paramMap) {
        Map<String, Object> condMap = parse();
        if (QueryUtil.isEmpty(condMap)) {
            return null;
        }

        List<Object> conditionList = new ArrayList<>();
        for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
            String key = entry.getKey();
            if (QueryUtil.isNotEmpty(key)) {
                Object cond = condMap.get(key);
                if (QueryUtil.isNotNull(cond)) {
                    Object condition = generateCondition(key, entry.getValue(), cond);
                    if (QueryUtil.isNotNull(condition)) {
                        conditionList.add(condition);
                    }
                }
            }
        }
        return new ReqQuery(operate, conditionList);
    }
    /**
     * <pre>
     * key:    name
     * value:  abc
     * cond:   $start
     * return: [ "name", "$start", "abc" ]
     *
     * key:    startTime
     * value:  xxxx-xx-xx xx:xx:xx
     * cond:   time:$ge
     * return: [ "time", "$ge", "xxxx-xx-xx xx:xx:xx" ]
     *
     * key:    y
     * value:  { "province": [ "x", "y", "z" ], "city": "xx" }
     * cond:   { "type": "or", "cons": { "province": "$in", "city": "$fuzzy" } }
     * return: { "operate": "or", "conditions": [ [ "province", "$in", [ "x", "y", "z" ] ], [ "city", "$fuzzy", "xx" ] ] }
     * </pre>
     */
    private Object generateCondition(String key, Object value, Object cond) {
        if (QueryUtil.isNotEmpty(key)) {
            if (cond instanceof String) {
                // $start    time:$ge
                String[] arr = QueryUtil.toStr(cond).split(":");
                int len = arr.length;
                if (len > 0) {
                    if (len == 1) {
                        return QueryUtil.isNull(value) ? Arrays.asList(key, arr[0]) : Arrays.asList(key, arr[0], value);
                    } else if (len == 2) {
                        return QueryUtil.isNull(value) ? Arrays.asList(arr[0], arr[1]) : Arrays.asList(arr[0], arr[1], value);
                    }
                }
            } else {
                // { "province": [ "x", "y", "z" ], "city": "xx" }
                Map<String, Object> data = QueryJsonUtil.convertData(value);
                Map<String, Object> templateQuery = QueryJsonUtil.convertData(cond);
                if (QueryUtil.isNotEmpty(templateQuery)) {
                    // or
                    OperateType type = OperateType.deserializer(templateQuery.get("type"));
                    // { "province": "$in", "city": "$fuzzy" }
                    Map<String, Object> composeMap = QueryJsonUtil.convertData(templateQuery.get("cons")); // conditions
                    if (QueryUtil.isNotNull(type) && QueryUtil.isNotEmpty(composeMap)) {
                        List<Object> composeConditionList = new ArrayList<>();
                        for (Map.Entry<String, Object> compose : composeMap.entrySet()) {
                            // province    city
                            String composeKey = compose.getKey();
                            // $in    $fuzzy
                            Object composeCond = compose.getValue();
                            if (QueryUtil.isNotEmpty(composeKey) || QueryUtil.isNotNull(composeCond)) {
                                // [ "x", "y", "z" ]    xx
                                Object composeValue = data.get(composeKey);
                                composeConditionList.add(generateCondition(composeKey, composeValue, composeCond));
                            }
                        }
                        return new ReqQuery(type, composeConditionList);
                    }
                }
            }
        }
        return null;
    }
    /**
     * {
     *   "name": "$start",
     *   "startTime": "time:$ge",
     *   "endTime": "time:$le",
     *   "x": {
     *     "type": "or",
     *     "cons": {
     *       "gender": "$eq",
     *       "age": "$bet"
     *     }
     *   },
     *   "y": {
     *     "type": "or",
     *     "cons": {
     *       "province": "$in",
     *       "city": "$fuzzy"
     *     }
     *   },
     *   "status": "$eq"
     * }
    */
    private Map<String, Object> parse() {
        Map<String, Object> returnMap = new HashMap<>();
        for (Object cond : conditions) {
            Map<String, Object> condition = QueryJsonUtil.convertData(cond);
            if (QueryUtil.isNotNull(condition)) {
                int size = condition.size();
                if (size == 1) {
                    returnMap.putAll(condition);
                } else if (size == 2) {
                    String metaNameKey = QueryConst.TEMPLATE_META_NAME;
                    String metaName = QueryUtil.toStr(condition.get(metaNameKey));
                    if (QueryUtil.isNotEmpty(metaName)) {
                        String join = null;
                        for (Map.Entry<String, Object> entry : condition.entrySet()) {
                            if (!metaNameKey.equals(entry.getKey())) {
                                join = entry.getKey() + ":" + entry.getValue();
                                break;
                            }
                        }
                        if (QueryUtil.isNotEmpty(join)) {
                            returnMap.put(metaName, join);
                        }
                    }
                } else {
                    ReqAliasTemplateQuery templateQuery = QueryJsonUtil.convert(cond, ReqAliasTemplateQuery.class);
                    if (QueryUtil.isNotNull(templateQuery)) {
                        String composeName = templateQuery.name;
                        String composeType = templateQuery.operate.name();
                        if (QueryUtil.isNotEmpty(composeName) && QueryUtil.isNotEmpty(composeType)) {
                            Map<String, Object> composeMap = templateQuery.parse();
                            if (QueryUtil.isNotEmpty(composeMap)) {
                                Map<String, Object> composeTypeMap = new HashMap<>();
                                composeTypeMap.put("type", composeType);
                                composeTypeMap.put("cons", composeMap); // conditions
                                returnMap.put(composeName, composeTypeMap);
                            }
                        }
                    }
                }
            }
        }
        return returnMap;
    }
}
