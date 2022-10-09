package com.github.liuanxin.query.web;

import com.github.liuanxin.query.config.QuerySchemaInfoHandler;
import com.github.liuanxin.query.model.QueryInfo;
import com.github.liuanxin.query.model.RequestInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/schema-column")
public class SchemaColumnController {

    @Value("${query.online:false}")
    private boolean online;

    private final QuerySchemaInfoHandler schemaInfoConfig;

    public SchemaColumnController(QuerySchemaInfoHandler schemaInfoConfig) {
        this.schemaInfoConfig = schemaInfoConfig;
    }

    @GetMapping
    public List<QueryInfo> info(String schemas) {
        return online ? Collections.emptyList() : schemaInfoConfig.info(schemas);
    }

    @PostMapping
    public Object query(@RequestBody RequestInfo req) {
        return req == null ? null : schemaInfoConfig.query(req);
    }
}
