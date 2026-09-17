package cn.tpl.opc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.tpl.opc.commons.dto.result.DeviceInstallPositionDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.scheme.request.QueryDeviceInstallPositionPageScheme;
import cn.tpl.opc.commons.scheme.request.SaveDeviceInstallPositionScheme;
import cn.tpl.opc.entity.DeviceInstallPositionEntity;
import cn.tpl.opc.entity.DeviceInfoEntity;
import cn.tpl.opc.mapper.DeviceInstallPositionEntityMapper;
import cn.tpl.opc.mapper.DeviceInfoEntityMapper;
import cn.tpl.opc.netty.Connection;
import cn.tpl.opc.netty.ConnectionMgr;
import cn.tpl.opc.netty.Connector;
import cn.tpl.opc.service.IDeviceInstallPositionService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service("deviceInstallPositionService")
public class DeviceInstallPositionServiceImpl implements IDeviceInstallPositionService {
    @Resource
    private DeviceInstallPositionEntityMapper mapper;
    @Resource
    private DeviceInfoEntityMapper deviceInfoEntityMapper;
    @Resource
    private ConnectionMgr connectionMgr;
    @Lazy
    @Resource
    private Connector connector;

    @Override
    public boolean save(SaveDeviceInstallPositionScheme scheme) {
        DeviceInstallPositionEntity entity = BeanUtil.copyProperties(scheme, DeviceInstallPositionEntity.class);
        if (entity.getSortNo() == null) entity.setSortNo(0);
        if (entity.getId() == null) {
            entity.setCreatedDate(new Date());
            return mapper.insertSelective(entity) > 0;
        }

        DeviceInstallPositionEntity oldEntity = mapper.selectByPrimaryKey(entity.getId());
        if (oldEntity == null) {
            throw new IllegalArgumentException("安装位置不存在！");
        }
        entity.setModifiedDate(new Date());
        boolean result = mapper.updateByPrimaryKeySelective(entity) > 0;
        if (result && !Objects.equals(oldEntity.getName(), entity.getName())) {
            Integer installSeq = entity.getId().intValue();
            deviceInfoEntityMapper.updatePositionByInstallSeq(installSeq, entity.getName());
            refreshConnectionsByInstallSeq(installSeq);
        }
        return result;
    }

    @Override
    public boolean deleteById(Long id) {
        if (id == null) return false;
        if (deviceInfoEntityMapper.countByInstallSeq(id.intValue()) > 0) {
            throw new IllegalArgumentException("该安装位置已被设备引用，不能删除！");
        }
        return mapper.deleteByPrimaryKey(id) > 0;
    }

    @Override
    public DeviceInstallPositionDTO findById(Long id) {
        return entity2DTO(findEntityById(id));
    }

    @Override
    public DeviceInstallPositionEntity findEntityById(Long id) {
        if (id == null) return null;
        return mapper.selectByPrimaryKey(id);
    }

    @Override
    public PageData<DeviceInstallPositionDTO> listByPage(QueryDeviceInstallPositionPageScheme scheme) {
        Page<DeviceInstallPositionEntity> page = new Page<>(scheme.getCurrentPage(), scheme.getPageSize());
        IPage<DeviceInstallPositionEntity> iPage = mapper.listByPage(page, scheme);
        return PageData.of(iPage, this::entity2DTO);
    }

    @Override
    public List<DeviceInstallPositionDTO> list() {
        return mapper.list().stream().map(this::entity2DTO).collect(Collectors.toList());
    }

    @Override
    public String getNameById(Integer id) {
        if (id == null) return null;
        DeviceInstallPositionEntity entity = findEntityById(id.longValue());
        return entity == null ? null : entity.getName();
    }

    private DeviceInstallPositionDTO entity2DTO(DeviceInstallPositionEntity entity) {
        if (entity == null) return null;
        return BeanUtil.copyProperties(entity, DeviceInstallPositionDTO.class);
    }

    private void refreshConnectionsByInstallSeq(Integer installSeq) {
        List<DeviceInfoEntity> devices = deviceInfoEntityMapper.listByInstallSeq(installSeq);
        for (DeviceInfoEntity device : devices) {
            refreshConnection(device);
        }
    }

    private void refreshConnection(DeviceInfoEntity device) {
        Long id = device.getId();
        if (id == null || !connectionMgr.connectionExists(id)) return;

        try {
            log.info("refreshConnectionByInstallPosition, refreshing device connection, id => {}", id);
            connectionMgr.removeConnection(id);

            Connection connection = new Connection();
            BeanUtil.copyProperties(device, connection);
            connector.connect(connection);
        } catch (Exception e) {
            log.error("refreshConnectionByInstallPosition failed, device id => {}", id, e);
        }
    }
}
