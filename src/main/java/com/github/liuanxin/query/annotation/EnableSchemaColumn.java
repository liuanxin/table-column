package com.github.liuanxin.query.annotation;

import com.github.liuanxin.query.web.SchemaColumnController;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({SchemaColumnController.class})
public @interface EnableSchemaColumn {
}
