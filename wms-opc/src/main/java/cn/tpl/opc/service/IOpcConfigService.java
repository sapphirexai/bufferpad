package cn.tpl.opc.service;

import cn.tpl.opc.commons.dto.result.OpcConfigDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.scheme.base.BasePageScheme;
import cn.tpl.opc.commons.scheme.request.SaveOpcConfigScheme;

public interface IOpcConfigService {
    OpcConfigDTO getOrInit();

    PageData<OpcConfigDTO> listByPage(BasePageScheme scheme);

    boolean save(SaveOpcConfigScheme scheme);

    boolean resetDefault(Long id);
}
