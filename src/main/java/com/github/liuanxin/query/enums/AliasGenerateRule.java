package com.github.liuanxin.query.enums;

/** if table-name has `user_info` or `USER_INFO`, column-name has `user_name` or `USER_NAME` */
public enum AliasGenerateRule {

    /** table-alias: UserInfo, column-alias: userName */
    Standard,

    Same,

    /** table-alias: User-Info, column-alias: user-name */
    Horizontal,

    Lower,

    Upper,

    /** table-alias: A-B...Z-AA...ZZ, column-alias: a-b...z-aa...zz */
    Letter
}
