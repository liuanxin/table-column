package com.github.liuanxin.query.enums;

/** if table name has `user_info` or `USER_INFO`, column name has `user_name` or `USER_NAME` */
public enum AliasGenerateRule {

    /** table alias: UserInfo, column alias: userName */
    Standard,

    /** table alias: User-Info, column alias: user-name */
    Horizontal,

    /** table alias: User_Info, column alias: user_name */
    Under,

    /** table alias: A-B...Z-AA...ZZ, column alias: a-b...z-aa...zz */
    Letter,

    Same,

    Lower,

    Upper
}
