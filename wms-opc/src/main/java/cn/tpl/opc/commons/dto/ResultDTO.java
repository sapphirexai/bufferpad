package cn.tpl.opc.commons.dto;

import cn.tpl.opc.commons.dto.base.AbsBaseDTO;
import cn.tpl.opc.commons.dto.EResultCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/6
 * 结果返回类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@Schema(description = "响应结构数据实体类")
public final class ResultDTO<T> extends AbsBaseDTO {
    private ResultDTO() {
    }

    @Schema(description = "响应码")
    private int code;

    @Schema(description = "响应消息")
    private String msg;

    @Schema(description = "具体数据")
    private T data;

    public ResultDTO(T data) {
        this(EResultCode.SUCCESS, data);
    }

    /**
     * 失败，设置对应的EResultCode和数据，数据不是必须的，看业务需要
     */
    public ResultDTO(EResultCode resultCode, T data, String msg) {
        if (resultCode == null) {
            throw new IllegalArgumentException("EResultCode is null");
        }
        this.code = resultCode.getCode();
        this.data = data;
        this.msg = StringUtils.isEmpty(msg) ? resultCode.getMsg() : msg;
    }

    /**
     * 失败，设置对应的EResultCode和数据，数据不是必须的，看业务需要
     */
    public ResultDTO(EResultCode resultCode, T data) {
        this(resultCode, data, null);
    }

    /**
     * 失败，设置对应的EResultCode和数据，数据不是必须的，看业务需要
     */
    public ResultDTO(EResultCode resultCode) {
        this(resultCode, null, null);
    }

    /**
     * 异常失败，可自定义消息
     */
    public ResultDTO(Exception e, String msg) {
        if (e == null) {
            throw new IllegalArgumentException("Exception is null");
        }
        this.code = EResultCode.EXCEPTION.getCode();
        this.msg = StringUtils.isEmpty(msg) ? e.getMessage() : msg;
    }

    /**
     * 异常失败
     */
    public ResultDTO(Exception e) {
        this(e, null);
    }

    /**
     * 判断是否业务成功，处理没有抛出异常，并且code为SUCCESS
     *
     * @see EResultCode#SUCCESS
     */
    public boolean isCodeSuccess() {
        return EResultCode.SUCCESS.getCode() == this.getCode();
    }

    public static <T> ResultDTO<T> success(T data) {
        return new ResultDTO<>(data);
    }

    public static <T> ResultDTO<T> failure(String msg) {
        return new ResultDTO<>(EResultCode.FAILURE, null, msg);
    }

    public static <T> ResultDTO<T> failure(T data, String msg) {
        return new ResultDTO<>(EResultCode.FAILURE, data, msg);
    }

    public static <T> ResultDTO<T> exception(Exception e) {
        e.printStackTrace();
        return new ResultDTO<>(EResultCode.EXCEPTION, null, e.getMessage());
    }

}
