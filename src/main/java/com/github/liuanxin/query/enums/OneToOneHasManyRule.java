package com.github.liuanxin.query.enums;

public enum OneToOneHasManyRule {

    /** default if not defined */
    Exception,

    /** if data has, ignore others */
    First,

    /** the back covers the front */
    Cover
}
