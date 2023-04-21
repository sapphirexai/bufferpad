package cn.tpl.opc.commons.dto.result;

import cn.tpl.opc.commons.dto.base.AbsBaseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/21
 * Sse消息数据数据实体类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "Sse消息数据数据实体类")
public class SseMsgDTO<T> extends AbsBaseDTO {
    /**
     * 消息主题
     *
     * @see cn.tpl.opc.commons.constant.Constants#SSE_MSG_TOPIC_DEVICE_STATUS
     */
    @Schema(description = "消息主题")
    private String topic;

    /**
     * 具体数据
     */
    private T data;

    public SseMsgDTO(String topic, T data) {
        this.topic = topic;
        this.data = data;
    }
}
