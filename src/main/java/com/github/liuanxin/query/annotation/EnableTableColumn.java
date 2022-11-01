package com.github.liuanxin.query.annotation;

import com.github.liuanxin.query.TableColumnConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({TableColumnConfig.class})
public @interface EnableTableColumn {
}
