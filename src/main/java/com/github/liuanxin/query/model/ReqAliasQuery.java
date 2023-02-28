package com.github.liuanxin.query.model;

import com.github.liuanxin.query.enums.OperateType;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <pre>
 * name like '...%'
 * and ( gender = ... or age between ... and ... )
 * and ( province in ( ... ) or city like '%...%' )
 * and time >= ...
 *
 *
 * {
 *   "operate": "and",
 *   "conditions": [
 *     { "name": "$start" },
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
 *     { "time": "$ge" }
 *   ]
 * }
 * </pre>
 */
public class ReqAliasQuery implements Serializable {
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
        ReqAliasQuery that = (ReqAliasQuery) o;
        return operate == that.operate && Objects.equals(conditions, that.conditions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operate, conditions);
    }
}
