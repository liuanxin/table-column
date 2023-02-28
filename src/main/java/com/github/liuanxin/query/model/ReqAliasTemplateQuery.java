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
}
