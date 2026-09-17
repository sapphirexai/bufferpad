package cn.tpl.opc.infrastructure.plc;

import lombok.Getter;

@Getter
public final class PlcIoResult<T> {
    private final boolean success;
    private final T content;
    private final int errorCode;
    private final String message;

    private PlcIoResult(boolean success, T content, int errorCode, String message) {
        this.success = success;
        this.content = content;
        this.errorCode = errorCode;
        this.message = message;
    }

    public static <T> PlcIoResult<T> success(T content) {
        return new PlcIoResult<>(true, content, 0, "");
    }

    public static <T> PlcIoResult<T> failure(int errorCode, String message) {
        return new PlcIoResult<>(false, null, errorCode, message == null ? "" : message);
    }
}
