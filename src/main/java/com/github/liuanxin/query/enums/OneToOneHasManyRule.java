package com.github.liuanxin.query.enums;

public enum OneToOneHasManyRule {

    // 0.抛出异常, 1.以前面的为准, 2.后面覆盖前面

    /** default if not defined */
    Exception,

    /** if data has, ignore others */
    First,

    /** the back covers the front */
    Cover
}
