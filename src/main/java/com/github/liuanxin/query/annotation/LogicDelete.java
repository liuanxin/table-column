package com.github.liuanxin.query.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogicDelete {

    /** logic delete: default value. for example: 0 */
    String value();

    /** logic delete: delete value. for example: 1, id, UNIX_TIMESTAMP() */
    String deleteValue();
}
