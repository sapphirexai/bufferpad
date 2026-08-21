package cn.tpl.opc.service.impl;

import HslCommunication.Core.Address.S7AddressData;
import HslCommunication.Core.Types.OperateResultExOne;
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
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/8/15
 * PLC寄存器地址服务
 */
@Service("plcAddrService")
public class PLCAddrServiceImpl implements IPLCAddrService {
    /**
     * S7 ANY pointers use 16 bits for the DB number and 24 bits for the bit
     * offset. A two-byte value therefore cannot start after byte 2,097,150.
     * HSL 3.4.0 truncates values outside those ranges, so validate them before
     * asking the driver to parse the address.
     */
    private static final Pattern SIEMENS_WORD_ADDRESS = Pattern.compile(
            "^(?:DB(\\d+)\\.DBW(\\d+)|[MIQ]W(\\d+))$");
    private static final long SIEMENS_MIN_DB_NUMBER = 1L;
    private static final long SIEMENS_MAX_DB_NUMBER = 65_535L;
    private static final long SIEMENS_MAX_WORD_BYTE_OFFSET = 2_097_150L;

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
        entity.setAddr(normalizeAndValidateAddress(scheme.getAddr(), plc.getType(), scheme.getType()));

        PLCAddrEntity duplicate = plcAddrEntityMapper.findDuplicate(entity);
        if (duplicate != null) {
            throw new IllegalArgumentException("同一扫码器的同一PLC操作类型不能重复配置！");
        }

        PLCAddrEntity differentPlc = plcAddrEntityMapper.findDifferentPlcByScannerId(entity);
        if (differentPlc != null) {
            throw new IllegalArgumentException("同一扫码器只能关联一个PLC！");
        }

        if (DeviceTypeEnum.isSiemensS7(plc.getType())) {
            validateNoSiemensAddressOverlap(entity);
        }

        if (entity.getId() == null) {
            return plcAddrEntityMapper.insertSelective(entity) > 0;
        }
        return plcAddrEntityMapper.updateByPrimaryKeySelective(entity) > 0;
    }

    private String normalizeAndValidateAddress(String rawAddress, Integer plcType, Integer plcAddrType) {
        if (rawAddress == null || rawAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("PLC地址不能为空！");
        }

        String address = rawAddress.trim();
        if (!DeviceTypeEnum.isSiemensS7(plcType)) return address;

        address = address.toUpperCase(Locale.ROOT);
        Matcher matcher = SIEMENS_WORD_ADDRESS.matcher(address);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "西门子S7地址必须是16位字地址，例如DB1.DBW0、MW0、IW0或QW0；不支持DBX、MX等位地址：" + address);
        }
        validateSiemensAddressRange(address, matcher);
        PlcAddrTypeEnum addressType = PlcAddrTypeEnum.of(plcAddrType);
        boolean isReadAddress = addressType == PlcAddrTypeEnum.SCAN_SUCCESS_OPEN_COUNT
                || addressType == PlcAddrTypeEnum.RE_SCAN_SUCCESS_OPEN_COUNT;
        if (!isReadAddress && address.startsWith("IW")) {
            throw new IllegalArgumentException("西门子IW输入区是只读地址，不能用于PLC写入操作：" + address);
        }
        OperateResultExOne<S7AddressData> result = S7AddressData.ParseFrom(address, 2);
        if (!result.IsSuccess) {
            String reason = result.Message == null || result.Message.trim().isEmpty()
                    ? "不支持的S7地址格式"
                    : result.Message;
            throw new IllegalArgumentException(
                    "西门子S7地址格式不正确：" + address + "。请使用DB1.DBW0、MW0、IW0或QW0等16位地址。原因：" + reason);
        }
        return address;
    }

    private void validateSiemensAddressRange(String address, Matcher matcher) {
        try {
            if (matcher.group(1) != null) {
                long dbNumber = Long.parseLong(matcher.group(1));
                if (dbNumber < SIEMENS_MIN_DB_NUMBER || dbNumber > SIEMENS_MAX_DB_NUMBER) {
                    throw new IllegalArgumentException(
                            "西门子S7 DB编号必须在1到65535之间：" + address);
                }
            }

            String offsetText = matcher.group(2) == null ? matcher.group(3) : matcher.group(2);
            long byteOffset = Long.parseLong(offsetText);
            if (byteOffset > SIEMENS_MAX_WORD_BYTE_OFFSET) {
                throw new IllegalArgumentException(
                        "西门子S7 16位字地址的字节偏移必须在0到2097150之间：" + address);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("西门子S7地址数值超出支持范围：" + address, e);
        }
    }

    private void validateNoSiemensAddressOverlap(PLCAddrEntity candidate) {
        SiemensWordRange candidateRange = toSiemensWordRange(candidate.getAddr());
        List<PLCAddrEntity> configuredAddresses = plcAddrEntityMapper.listByPlcId(candidate.getPlcId());
        if (configuredAddresses == null || configuredAddresses.isEmpty()) return;

        for (PLCAddrEntity configured : configuredAddresses) {
            if (configured == null || Objects.equals(candidate.getId(), configured.getId())) continue;
            SiemensWordRange configuredRange = toSiemensWordRange(configured.getAddr());
            if (configuredRange != null && candidateRange.overlaps(configuredRange)) {
                throw new IllegalArgumentException("西门子S7地址与同一PLC的已有地址发生2字节重叠："
                        + candidate.getAddr() + " 与 " + configured.getAddr());
            }
        }
    }

    private SiemensWordRange toSiemensWordRange(String address) {
        if (address == null) return null;
        String normalized = address.trim().toUpperCase(Locale.ROOT);
        Matcher matcher = SIEMENS_WORD_ADDRESS.matcher(normalized);
        if (!matcher.matches()) return null;
        try {
            if (matcher.group(1) != null) {
                return new SiemensWordRange("DB", Long.parseLong(matcher.group(1)),
                        Long.parseLong(matcher.group(2)));
            }
            return new SiemensWordRange(normalized.substring(0, 1), 0L,
                    Long.parseLong(matcher.group(3)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final class SiemensWordRange {
        private final String area;
        private final long dbNumber;
        private final long byteOffset;

        private SiemensWordRange(String area, long dbNumber, long byteOffset) {
            this.area = area;
            this.dbNumber = dbNumber;
            this.byteOffset = byteOffset;
        }

        private boolean overlaps(SiemensWordRange other) {
            if (!area.equals(other.area) || dbNumber != other.dbNumber) return false;
            long end = byteOffset + 1;
            long otherEnd = other.byteOffset + 1;
            return byteOffset <= otherEnd && other.byteOffset <= end;
        }
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
