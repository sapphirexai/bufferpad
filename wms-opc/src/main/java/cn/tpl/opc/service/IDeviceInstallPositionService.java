package cn.tpl.opc.service;

import cn.tpl.opc.commons.dto.result.DeviceInstallPositionDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.scheme.request.QueryDeviceInstallPositionPageScheme;
import cn.tpl.opc.commons.scheme.request.SaveDeviceInstallPositionScheme;
import cn.tpl.opc.entity.DeviceInstallPositionEntity;

import java.util.List;

public interface IDeviceInstallPositionService {
    boolean save(SaveDeviceInstallPositionScheme scheme);

    boolean deleteById(Long id);

    DeviceInstallPositionDTO findById(Long id);

    DeviceInstallPositionEntity findEntityById(Long id);

    PageData<DeviceInstallPositionDTO> listByPage(QueryDeviceInstallPositionPageScheme scheme);

    List<DeviceInstallPositionDTO> list();

    String getNameById(Integer id);
}
