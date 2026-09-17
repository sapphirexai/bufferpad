package cn.tpl.opc.commons.dto.enums;

import cn.tpl.opc.commons.dto.result.OptionDTO;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 设备类型。
 */
public enum DeviceTypeEnum {
    SCANNER(0, "扫码器"),
    MITSUBISHI_PLC(1, "三菱PLC"),
    INOVANCE_PLC(2, "汇川PLC"),
    SIEMENS_S7_PLC(3, "西门子 S7 PLC"),
    /**
     * Historical S7-1500 code. Existing records are migrated to code 3, but
     * retaining the alias prevents an interrupted upgrade from losing PLC
     * connectivity before its database migration has completed.
     */
    @Deprecated
    LEGACY_SIEMENS_S7_1500_PLC(4, "西门子 S7 PLC");

    private final Integer code;
    private final String label;

    DeviceTypeEnum(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public Integer getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public boolean isPlc() {
        return this == MITSUBISHI_PLC
                || this == INOVANCE_PLC
                || this == SIEMENS_S7_PLC
                || this == LEGACY_SIEMENS_S7_1500_PLC;
    }

    public boolean isSiemensS7() {
        return this == SIEMENS_S7_PLC || this == LEGACY_SIEMENS_S7_1500_PLC;
    }

    public boolean isLegacy() {
        return this == LEGACY_SIEMENS_S7_1500_PLC;
    }

    public static DeviceTypeEnum of(Integer code) {
        if (code == null) return null;
        return Arrays.stream(values())
                .filter(item -> Objects.equals(item.code, code))
                .findFirst()
                .orElse(null);
    }

    public static boolean isScanner(Integer code) {
        return Objects.equals(SCANNER.code, code);
    }

    public static boolean isPlc(Integer code) {
        DeviceTypeEnum type = of(code);
        return type != null && type.isPlc();
    }

    public static boolean isSiemensS7(Integer code) {
        DeviceTypeEnum type = of(code);
        return type != null && type.isSiemensS7();
    }

    public static List<OptionDTO<Integer>> options() {
        return Arrays.stream(values())
                .filter(item -> !item.isLegacy())
                .map(item -> new OptionDTO<>(item.code, item.label))
                .collect(Collectors.toList());
    }
}
