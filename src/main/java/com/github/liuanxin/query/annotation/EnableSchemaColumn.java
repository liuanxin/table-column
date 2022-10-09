package com.github.liuanxin.query.annotation;

import com.github.liuanxin.query.config.QuerySchemaInfoHandler;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({QuerySchemaInfoHandler.class})
public @interface EnableSchemaColumn {
}
