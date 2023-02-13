package com.github.liuanxin.query.enums;

/** if table name has `user_info` or `USER_INFO`, column name has `user_name` or `USER_NAME` */
public enum AliasGenerateRule {

    /** default if not defined. table alias: UserInfo, column alias: userName */
    Standard,

    /** table alias: User-Info, column alias: user-name */
    Horizontal,

    /** table alias: User_Info, column alias: user_name */
    Under,

    /** table alias: A-B...Z-AA...ZZ, column alias: a-b...z-aa...zz */
    Letter,

    /** table alias: 100001-100002..., column alias: 1-2... */
    Number,

    Same,

    /** table alias: user_info, column alias: user_name */
    Lower,

    /** table alias: USER_INFO, column alias: USER_NAME */
    Upper
}
