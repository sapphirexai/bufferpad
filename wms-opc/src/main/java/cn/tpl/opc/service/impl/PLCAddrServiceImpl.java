package cn.tpl.opc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.tpl.opc.commons.dto.enums.DeviceTypeEnum;
import cn.tpl.opc.commons.dto.enums.PlcAddrTypeEnum;
import cn.tpl.opc.commons.dto.result.DeviceInfoDTO;
import cn.tpl.opc.commons.dto.result.PLCAddrDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.scheme.request.QueryPLCAddrPageScheme;
import cn.tpl.opc.commons.scheme.request.SavePLCAddrScheme;
import cn.tpl.opc.entity.DeviceInfoEntity;
import cn.tpl.opc.entity.PLCAddrEntity;
import cn.tpl.opc.mapper.PLCAddrEntityMapper;
import cn.tpl.opc.service.IDeviceInstallPositionService;
import cn.tpl.opc.service.IDeviceInfoService;
import cn.tpl.opc.service.IPLCAddrService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/8/15
 * PLC寄存器地址服务
 */
@Service("plcAddrService")
public class PLCAddrServiceImpl implements IPLCAddrService {
    @Resource
    private PLCAddrEntityMapper plcAddrEntityMapper;
    @Resource
    private IDeviceInfoService deviceInfoService;
    @Resource
    private IDeviceInstallPositionService installPositionService;

    @Transactional
    @Override
    public PLCAddrEntity findByTypeAndScannerId(Integer type, Long scannerId) {
        if (null == type || null == scannerId) return null;

        PLCAddrEntity entity = new PLCAddrEntity();
        entity.setType(type);
        entity.setScannerId(scannerId);
        return plcAddrEntityMapper.findByTypeAndScannerId(entity);
    }

    @Override
    public List<PLCAddrEntity> listByPlcIdAndType(Long plcId, Integer type) {
        PLCAddrEntity entity = new PLCAddrEntity();
        entity.setPlcId(plcId);
        entity.setType(type);
        return plcAddrEntityMapper.listByPlcIdAndType(entity);
    }

    @Override
    public PageData<PLCAddrDTO> listByPage(QueryPLCAddrPageScheme scheme) {
        Page<PLCAddrEntity> page = new Page<>(scheme.getCurrentPage(), scheme.getPageSize());
        IPage<PLCAddrEntity> iPage = plcAddrEntityMapper.listByPage(page, scheme);
        return PageData.of(iPage, this::entity2DTO);
    }

    @Override
    public PLCAddrDTO findById(Long id) {
        if (id == null) return null;
        return entity2DTO(plcAddrEntityMapper.selectByPrimaryKey(id));
    }

    @Override
    public boolean save(SavePLCAddrScheme scheme) {
        if (!PlcAddrTypeEnum.exists(scheme.getType())) {
            throw new IllegalArgumentException("PLC地址类型不存在！");
        }

        DeviceInfoDTO plc = deviceInfoService.findById(scheme.getPlcId());
        if (plc == null || !DeviceTypeEnum.isPlc(plc.getType())) {
            throw new IllegalArgumentException("PLC设备不存在或类型不正确！");
        }

        DeviceInfoDTO scanner = deviceInfoService.findById(scheme.getScannerId());
        if (scanner == null || !DeviceTypeEnum.isScanner(scanner.getType())) {
            throw new IllegalArgumentException("扫码器不存在或类型不正确！");
        }

        if (!Objects.equals(plc.getWorkLine(), scanner.getWorkLine())) {
            throw new IllegalArgumentException("PLC与扫码器必须属于同一产线！");
        }

        PLCAddrEntity entity = BeanUtil.copyProperties(scheme, PLCAddrEntity.class);

        PLCAddrEntity duplicate = plcAddrEntityMapper.findDuplicate(entity);
        if (duplicate != null) {
            throw new IllegalArgumentException("同一扫码器的同一PLC操作类型不能重复配置！");
        }

        PLCAddrEntity differentPlc = plcAddrEntityMapper.findDifferentPlcByScannerId(entity);
        if (differentPlc != null) {
            throw new IllegalArgumentException("同一扫码器只能关联一个PLC！");
        }

        if (entity.getId() == null) {
            return plcAddrEntityMapper.insertSelective(entity) > 0;
        }
        return plcAddrEntityMapper.updateByPrimaryKeySelective(entity) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return plcAddrEntityMapper.deleteByPrimaryKey(id) > 0;
    }

    private PLCAddrDTO entity2DTO(PLCAddrEntity entity) {
        if (entity == null) return null;

        PLCAddrDTO dto = BeanUtil.copyProperties(entity, PLCAddrDTO.class);
        dto.setTypeName(PlcAddrTypeEnum.labelOf(entity.getType()));

        DeviceInfoDTO plc = entity.getPlcId() == null ? null : deviceInfoService.findById(entity.getPlcId());
        if (plc != null) dto.setPlcName(plc.getName());

        DeviceInfoDTO scanner = entity.getScannerId() == null ? null : deviceInfoService.findById(entity.getScannerId());
        if (scanner != null) {
            dto.setScannerName(scanner.getName());
            if (scanner.getInstallSeq() != null) {
                dto.setInstallPositionId(scanner.getInstallSeq().longValue());
                dto.setInstallPositionName(installPositionService.getNameById(scanner.getInstallSeq()));
            }
        }
        return dto;
    }
}
