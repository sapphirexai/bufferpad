package cn.tpl.opc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.tpl.opc.commons.dto.enums.DeviceTypeEnum;
import cn.tpl.opc.commons.dto.result.DeviceInfoDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.scheme.request.QueryDeviceInfoPageScheme;
import cn.tpl.opc.commons.scheme.request.SaveDeviceInfoScheme;
import cn.tpl.opc.entity.DeviceInfoEntity;
import cn.tpl.opc.mapper.DeviceInfoEntityMapper;
import cn.tpl.opc.mapper.PLCAddrEntityMapper;
import cn.tpl.opc.service.IDeviceInstallPositionService;
import cn.tpl.opc.service.IDeviceInfoService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/10
 */
@Service("deviceService")
public class DeviceInfoServiceImpl implements IDeviceInfoService {
    @Resource
    private DeviceInfoEntityMapper deviceInfoEntityMapper;
    @Resource
    private PLCAddrEntityMapper plcAddrEntityMapper;
    @Resource
    private IDeviceInstallPositionService installPositionService;

    @Override
    public boolean save(SaveDeviceInfoScheme scheme) {
        if (DeviceTypeEnum.of(scheme.getType()) == null) {
            throw new IllegalArgumentException("设备类型不存在！");
        }
        if (installPositionService.findEntityById(scheme.getInstallSeq().longValue()) == null) {
            throw new IllegalArgumentException("安装位置不存在！");
        }

        DeviceInfoEntity entity = BeanUtil.copyProperties(scheme, DeviceInfoEntity.class);
        entity.setPosition(installPositionService.getNameById(entity.getInstallSeq()));
        if (entity.getStatus() == null) entity.setStatus(0);

        if (entity.getId() == null) {
            if (entity.getWorkLine() == null) entity.setWorkLine(1);
            entity.setCreatedDate(new Date());
            return deviceInfoEntityMapper.insertSelective(entity) > 0;
        }

        DeviceInfoEntity oldEntity = deviceInfoEntityMapper.selectByPrimaryKey(entity.getId());
        if (oldEntity == null) {
            throw new IllegalArgumentException("设备不存在！");
        }
        if (entity.getWorkLine() == null) entity.setWorkLine(oldEntity.getWorkLine());
        checkDeviceTypeChange(oldEntity, entity);
        checkDeviceWorkLineChange(oldEntity, entity);

        entity.setModifiedDate(new Date());
        return deviceInfoEntityMapper.updateByPrimaryKeySelective(entity) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        DeviceInfoEntity entity = deviceInfoEntityMapper.selectByPrimaryKey(id);
        if (entity == null) return false;
        checkDeviceNotReferenced(entity);
        return deviceInfoEntityMapper.deleteByPrimaryKey(id) > 0;
    }

    @Override
    public DeviceInfoDTO findById(Long id) {
        if (id == null) return null;
        return deviceInfo2DTO(deviceInfoEntityMapper.selectByPrimaryKey(id));
    }

    @Override
    public List<DeviceInfoEntity> listDeviceInfoByType(Integer type) {
        return deviceInfoEntityMapper.listDeviceInfoByType(type);
    }

    @Override
    public PageData<DeviceInfoDTO> listByPage(QueryDeviceInfoPageScheme scheme) {
        Page<DeviceInfoEntity> page = new Page<>(scheme.getCurrentPage(), scheme.getPageSize());
        IPage<DeviceInfoEntity> iPage = deviceInfoEntityMapper.listByPage(page, scheme);
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
        BeanUtil.copyProperties(deviceInfo, deviceInfoDTO);
        DeviceTypeEnum deviceType = DeviceTypeEnum.of(deviceInfo.getType());
        deviceInfoDTO.setTypeName(deviceType == null ? "" : deviceType.getLabel());
        deviceInfoDTO.setInstallPositionName(installPositionService.getNameById(deviceInfo.getInstallSeq()));
        return deviceInfoDTO;
    }

    private void checkDeviceTypeChange(DeviceInfoEntity oldEntity, DeviceInfoEntity newEntity) {
        boolean oldIsPlc = DeviceTypeEnum.isPlc(oldEntity.getType());
        boolean newIsPlc = DeviceTypeEnum.isPlc(newEntity.getType());
        if (oldIsPlc && !newIsPlc && plcAddrEntityMapper.countByPlcId(oldEntity.getId()) > 0) {
            throw new IllegalArgumentException("该PLC已被PLC地址配置引用，不能修改为非PLC类型！");
        }
        boolean sameSiemensProtocol = DeviceTypeEnum.isSiemensS7(oldEntity.getType())
                && DeviceTypeEnum.isSiemensS7(newEntity.getType());
        if (oldIsPlc && newIsPlc && !Objects.equals(oldEntity.getType(), newEntity.getType())
                && !sameSiemensProtocol
                && plcAddrEntityMapper.countByPlcId(oldEntity.getId()) > 0) {
            throw new IllegalArgumentException("该PLC已被地址配置引用，不能直接修改通信协议类型；请先删除对应PLC地址配置！");
        }

        boolean oldIsScanner = DeviceTypeEnum.isScanner(oldEntity.getType());
        boolean newIsScanner = DeviceTypeEnum.isScanner(newEntity.getType());
        if (oldIsScanner && !newIsScanner && plcAddrEntityMapper.countByScannerId(oldEntity.getId()) > 0) {
            throw new IllegalArgumentException("该扫码器已被PLC地址配置引用，不能修改为非扫码器类型！");
        }
    }

    private void checkDeviceWorkLineChange(DeviceInfoEntity oldEntity, DeviceInfoEntity newEntity) {
        if (Objects.equals(oldEntity.getWorkLine(), newEntity.getWorkLine())) return;

        if (DeviceTypeEnum.isPlc(oldEntity.getType()) && plcAddrEntityMapper.countByPlcId(oldEntity.getId()) > 0) {
            throw new IllegalArgumentException("该PLC已被PLC地址配置引用，不能修改产线！");
        }

        if (DeviceTypeEnum.isScanner(oldEntity.getType()) && plcAddrEntityMapper.countByScannerId(oldEntity.getId()) > 0) {
            throw new IllegalArgumentException("该扫码器已被PLC地址配置引用，不能修改产线！");
        }
    }

    private void checkDeviceNotReferenced(DeviceInfoEntity entity) {
        if (DeviceTypeEnum.isPlc(entity.getType()) && plcAddrEntityMapper.countByPlcId(entity.getId()) > 0) {
            throw new IllegalArgumentException("该PLC已被PLC地址配置引用，不能删除！");
        }

        if (DeviceTypeEnum.isScanner(entity.getType()) && plcAddrEntityMapper.countByScannerId(entity.getId()) > 0) {
            throw new IllegalArgumentException("该扫码器已被PLC地址配置引用，不能删除！");
        }
    }
}
