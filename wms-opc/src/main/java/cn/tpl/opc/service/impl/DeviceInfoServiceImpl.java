package cn.tpl.opc.service.impl;

import cn.tpl.opc.commons.dto.result.DeviceInfoDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.scheme.base.BasePageScheme;
import cn.tpl.opc.entity.DeviceInfoEntity;
import cn.tpl.opc.mapper.DeviceInfoEntityMapper;
import cn.tpl.opc.service.IDeviceInfoService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/10
 */
@Service("deviceService")
public class DeviceInfoServiceImpl implements IDeviceInfoService {
    @Resource
    private DeviceInfoEntityMapper deviceInfoEntityMapper;

    public boolean deleteById(Long id) {
        return deviceInfoEntityMapper.deleteById(id) > 0;
    }

    @Override
    public DeviceInfoDTO findById(Long id) {
        return deviceInfo2DTO(deviceInfoEntityMapper.selectById(id));
    }

    @Override
    public List<DeviceInfoEntity> listDeviceInfoByType(Integer type) {
        return deviceInfoEntityMapper.listDeviceInfoByType(type);
    }

    @Override
    public PageData<DeviceInfoDTO> listByPage(BasePageScheme scheme) {
        Page<DeviceInfoEntity> page = new Page<>(scheme.getCurrentPage(), scheme.getPageSize());
        IPage<DeviceInfoEntity> iPage = deviceInfoEntityMapper.listByPage(page);
        List<DeviceInfoEntity> deviceInfos = iPage.getRecords();

        if (CollectionUtils.isEmpty(deviceInfos)) return null;
        return PageData.of(iPage, this::deviceInfo2DTO);
    }

    @Override
    public List<DeviceInfoEntity> list() {
        return deviceInfoEntityMapper.list();
    }

    @Override
    public List<DeviceInfoEntity> listByWorkLine(Integer workLine) {
        return deviceInfoEntityMapper.listByWorkLine(workLine);
    }

    private DeviceInfoDTO deviceInfo2DTO(DeviceInfoEntity deviceInfo) {
        if (null == deviceInfo) return null;
        DeviceInfoDTO deviceInfoDTO = new DeviceInfoDTO();
        BeanUtils.copyProperties(deviceInfo, deviceInfoDTO);
        return deviceInfoDTO;
    }
}
