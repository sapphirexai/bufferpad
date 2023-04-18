package cn.tpl.opc.commons.dto;

import cn.tpl.opc.commons.dto.base.IResultEnumCode;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/6
 * 请求结果Code
 */
public enum EResultCode implements IResultEnumCode {
    SUCCESS(0, "操作成功"),
    FAILURE(1, "操作失败"),
    EXCEPTION(500, "服务异常");

    private final int code;
    private final String msg;

    EResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public int getCode() {
        return this.code;
    }

    @Override
    public String getMsg() {
        return this.msg;
    }
}
