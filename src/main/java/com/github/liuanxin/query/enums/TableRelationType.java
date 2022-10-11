package com.github.liuanxin.query.enums;

public enum TableRelationType {

    NULL,

    ONE_TO_ONE,
    ONE_TO_MANY,
    // MANY_TO_MANY
    ;

    public boolean hasMany() {
        return this != NULL && this != ONE_TO_ONE;
    }
}
