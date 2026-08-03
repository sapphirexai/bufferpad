package cn.tpl.opc.commons.dto.enums;

/**
 * Stable operation event codes shared by backend and frontend.
 */
public enum OperationEventCode {
    SCAN_COUNTED("INFO", "扫码计数完成", "缓冲垫已完成计数", ""),
    SCAN_NO_READ("WARNING", "扫码失败", "读码器未识别到二维码", "请重新扫码，仍失败时可使用手动输入"),
    SCAN_REPEATED("WARNING", "扫码未计数", "两小时内重复扫码，本次未增加使用次数", "无需重复扫码，如需补录请确认现场流程"),
    CUSHION_MAX_REACHED("ERROR", "缓冲垫达到寿命", "缓冲垫使用次数已达到上限", "请从回流线上撤走该缓冲垫"),
    SCAN_COUNT_FAILED("ERROR", "计数失败", "缓冲垫数据未能保存", "请暂停操作并联系系统维护人员"),
    PLC_ADDRESS_NOT_CONFIGURED("WARNING", "PLC地址未配置", "缓冲垫已处理，但没有找到对应的PLC地址", "请在设置中的PLC地址页面补充配置"),
    PLC_TARGET_NOT_RESOLVED("WARNING", "PLC目标无法确定", "手动扫码未关联到具体扫码器，系统没有发送PLC指令", "请在对应工位扫码器上重新扫码，或先确认缓冲垫已关联扫码器"),
    PLC_READ_ADDRESS_NOT_CONFIGURED("WARNING", "开口数地址未配置", "缓冲垫计数不受影响，但无法读取PLC开口数", "如需显示开口数，请补充对应的PLC回读地址"),
    PLC_OFFLINE("ERROR", "PLC未连接", "缓冲垫已处理，但PLC当前不可用", "请检查PLC电源、网线、IP和通信端口"),
    PLC_NOTIFY_SUCCEEDED("INFO", "PLC通知成功", "PLC已收到本次处理结果", ""),
    PLC_WRITE_REJECTED("ERROR", "PLC拒绝写入", "缓冲垫已处理，但PLC拒绝执行写入", "请联系设备人员检查PLC运行中写入权限和寄存器配置"),
    PLC_WRITE_FAILED("ERROR", "PLC通知失败", "缓冲垫已处理，但PLC通信失败", "请检查PLC网络和通信参数"),
    PLC_READ_FAILED("WARNING", "开口数读取失败", "缓冲垫计数和PLC通知已完成，但开口数读取失败", "请检查开口数寄存器地址和PLC读取权限");

    private final String severity;
    private final String title;
    private final String defaultMessage;
    private final String suggestion;

    OperationEventCode(String severity, String title, String defaultMessage, String suggestion) {
        this.severity = severity;
        this.title = title;
        this.defaultMessage = defaultMessage;
        this.suggestion = suggestion;
    }

    public String getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public String getSuggestion() {
        return suggestion;
    }
}
