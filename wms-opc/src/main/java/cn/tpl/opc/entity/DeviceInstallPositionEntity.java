package cn.tpl.opc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 设备安装位置。
 */
@TableName("device_install_position")
@Data
public class DeviceInstallPositionEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID。
     */
    private Long id;

    /**
     * 位置名称，例如：上、下、间层1。
     */
    private String name;

    /**
     * 排序号。
     */
    private Integer sortNo;

    /**
     * 创建时间。
     */
    private Date createdDate;

    /**
     * 更新时间。
     */
    private Date modifiedDate;
}
