package cn.tpl.opc.commons.scheme.request;

import cn.tpl.opc.commons.scheme.base.AbsBaseScheme;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "手动扫码请求")
public class ManualCushionInfoScheme extends AbsBaseScheme {
    @NotNull(message = "产线不能为空！")
    @Schema(description = "产线")
    private Integer workLine;

    @NotBlank(message = "缓冲垫编码不能为空！")
    @Schema(description = "二维码")
    private String qrCode;
}
