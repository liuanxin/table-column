package com.github.liuanxin.query.model;

import com.github.liuanxin.query.enums.OperateType;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private List<Map<String, Object>> conditions;

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

    public List<Map<String, Object>> getConditions() {
        return conditions;
    }
    public void setConditions(List<Map<String, Object>> conditions) {
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
     *     [ "status", "$ge", 1 ]
     *   ]
     * }
     *
     * 其生成的查询是
     * name like 'abc%'
     * and time >= 'xxxx-xx-xx xx:xx:xx'
     * and time <= 'yyyy-yy-yy yy:yy:yy'
     * and ( gender = 1 or age between 18 and 40 )
     * and ( province in ( 'x', 'y', 'z' ) or city like '%xx%' )
     * and status = 1
     * </pre>
     */
    public ReqQuery handle(Map<String, Object> paramMap) {
        ReqQuery query = new ReqQuery();
        for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
        }
        // todo
        return query;
    }
}
