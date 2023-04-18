package cn.tpl.opc.commons.dto.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CushionCode {

    CONNECTING("0","正在连接"),
    CONNECTED("1","已连接"),
    CONNECTDISABLE("2","连接失败"),
    OUTLINE("3","掉线");

    @JsonValue
    private String key;

    private String text;

    CushionCode(String key, String text) {
        this.key = key;
        this.text = text;

    }

}
