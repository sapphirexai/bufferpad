package cn.tpl.opc.mapper;

import cn.tpl.opc.commons.scheme.request.QueryDeviceInstallPositionPageScheme;
import cn.tpl.opc.entity.DeviceInstallPositionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeviceInstallPositionEntityMapper extends BaseMapper<DeviceInstallPositionEntity> {
    int deleteByPrimaryKey(Long id);

    int insert(DeviceInstallPositionEntity record);

    int insertSelective(DeviceInstallPositionEntity record);

    DeviceInstallPositionEntity selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(DeviceInstallPositionEntity record);

    int updateByPrimaryKey(DeviceInstallPositionEntity record);

    IPage<DeviceInstallPositionEntity> listByPage(@Param("page") Page<DeviceInstallPositionEntity> page,
                                                  @Param("data") QueryDeviceInstallPositionPageScheme scheme);

    List<DeviceInstallPositionEntity> list();
}
