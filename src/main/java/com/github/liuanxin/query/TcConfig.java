package com.github.liuanxin.query;

import com.github.liuanxin.query.core.TableColumnTemplate;
import com.github.liuanxin.query.model.TableColumnRelation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class TcConfig {

    @Bean
    @ConditionalOnMissingBean
    public List<TableColumnRelation> relationList() {
        return new ArrayList<>();
    }

    @Bean
    public TableColumnTemplate tableColumnTemplate(JdbcTemplate jdbcTemplate, List<TableColumnRelation> relationList) {
        return new TableColumnTemplate(jdbcTemplate, relationList);
    }
}
