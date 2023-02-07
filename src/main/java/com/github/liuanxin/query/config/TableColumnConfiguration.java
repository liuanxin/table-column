package com.github.liuanxin.query.config;

import com.github.liuanxin.query.core.TableColumnTemplate;
import com.github.liuanxin.query.model.ReqModel;
import com.github.liuanxin.query.model.TableColumnRelation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class TableColumnConfiguration {

    @Bean("tableRelationList")
    @ConditionalOnMissingBean
    public List<TableColumnRelation> tableRelationList() {
        return new ArrayList<>();
    }

    @Bean("queryAliasMap")
    @ConditionalOnMissingBean
    public Map<String, ReqModel> queryAliasMap() {
        return new HashMap<>();
    }

    @Bean
    public TableColumnTemplate tableColumnTemplate(JdbcTemplate jdbcTemplate,
                                                   List<TableColumnRelation> tableRelationList,
                                                   Map<String, ReqModel> tableAliasMap) {
        return new TableColumnTemplate(jdbcTemplate, tableRelationList, tableAliasMap);
    }
}
