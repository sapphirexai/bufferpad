package cn.tpl.opc.commons.dto.enums;

import cn.tpl.opc.commons.dto.result.OptionDTO;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * PLC 地址操作类型。
 */
public enum PlcAddrTypeEnum {
    SCAN_FAILED(0, "扫码失败"),
    SCAN_OVER_MAXIMUM(1, "扫码超过最大次数"),
    SCAN_SUCCESS(2, "扫码成功"),
    RE_SCAN_OVER_MAXIMUM(3, "重新扫码超过最大次数"),
    RE_SCAN_SUCCESS(4, "重新扫码成功"),
    HEART_BEAT(5, "心跳"),
    SCAN_SUCCESS_OPEN_COUNT(6, "扫码成功开口数回读"),
    RE_SCAN_SUCCESS_OPEN_COUNT(7, "重新扫码成功开口数回读");

    private final Integer code;
    private final String label;

    PlcAddrTypeEnum(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public Integer getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static PlcAddrTypeEnum of(Integer code) {
        if (code == null) return null;
        return Arrays.stream(values())
                .filter(item -> Objects.equals(item.code, code))
                .findFirst()
                .orElse(null);
    }

    public static boolean exists(Integer code) {
        return of(code) != null;
    }

    public static String labelOf(Integer code) {
        PlcAddrTypeEnum type = of(code);
        return type == null ? "" : type.label;
    }

    public static List<OptionDTO<Integer>> options() {
        return Arrays.stream(values())
                .map(item -> new OptionDTO<>(item.code, item.label))
                .collect(Collectors.toList());
    }
}
