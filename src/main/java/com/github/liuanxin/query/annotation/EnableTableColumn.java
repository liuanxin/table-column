package com.github.liuanxin.query.annotation;

import com.github.liuanxin.query.core.TableColumnTemplate;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({TableColumnTemplate.class})
public @interface EnableTableColumn {
}
