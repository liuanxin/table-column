package com.github.liuanxin.query.config;

import com.github.liuanxin.query.core.TableColumnTemplate;
import com.github.liuanxin.query.model.RequestModel;
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

    @Bean
    @ConditionalOnMissingBean
    public List<TableColumnRelation> relationList() {
        return new ArrayList<>();
    }

    @Bean
    @ConditionalOnMissingBean
    public Map<String, RequestModel> requestAliasMap() {
        return new HashMap<>();
    }

    @Bean
    public TableColumnTemplate tableColumnTemplate(JdbcTemplate jdbcTemplate,
                                                   List<TableColumnRelation> relationList,
                                                   Map<String, RequestModel> requestAliasMap) {
        return new TableColumnTemplate(jdbcTemplate, relationList, requestAliasMap);
    }
}
