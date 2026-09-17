package cn.tpl.opc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.dto.result.OpcConfigDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.scheme.base.BasePageScheme;
import cn.tpl.opc.commons.scheme.request.SaveOpcConfigScheme;
import cn.tpl.opc.entity.OpcConfigEntity;
import cn.tpl.opc.mapper.OpcConfigEntityMapper;
import cn.tpl.opc.service.IOpcConfigService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;

@Service("opcConfigService")
public class OpcConfigServiceImpl implements IOpcConfigService {
    @Resource
    private OpcConfigEntityMapper mapper;

    @Override
    public OpcConfigDTO getOrInit() {
        return entity2DTO(getOrInitEntity());
    }

    @Override
    public PageData<OpcConfigDTO> listByPage(BasePageScheme scheme) {
        PageData<OpcConfigDTO> pageData = new PageData<>();
        pageData.setCurrentPage(scheme.getCurrentPage());
        pageData.setPageSize(scheme.getPageSize());
        pageData.setTotalPage(1);
        pageData.setData(Collections.singletonList(getOrInit()));
        return pageData;
    }

    @Override
    public boolean save(SaveOpcConfigScheme scheme) {
        Long id = scheme.getId();
        if (id != null && id != Constants.OPC_CONFIG_ID) return false;
        if (scheme.getCushionMaxUseCount() == null || scheme.getCushionMaxUseCount() <= 0) {
            throw new IllegalArgumentException("缓冲垫最大使用数量必须大于0！");
        }

        OpcConfigEntity entity = new OpcConfigEntity();
        entity.setId(Constants.OPC_CONFIG_ID);
        entity.setCushionMaxUseCount(scheme.getCushionMaxUseCount());
        getOrInitEntity();
        return mapper.updateByPrimaryKeySelective(entity) > 0;
    }

    @Override
    public boolean resetDefault(Long id) {
        if (id != null && id != Constants.OPC_CONFIG_ID) return false;
        SaveOpcConfigScheme scheme = new SaveOpcConfigScheme();
        scheme.setId(Constants.OPC_CONFIG_ID);
        scheme.setCushionMaxUseCount(Constants.CUSHION_DEFAULT_MAX_USE_CONT);
        return save(scheme);
    }

    private synchronized OpcConfigEntity getOrInitEntity() {
        OpcConfigEntity entity = mapper.selectByPrimaryKey(Constants.OPC_CONFIG_ID);
        if (entity != null) return entity;

        entity = new OpcConfigEntity();
        entity.setId(Constants.OPC_CONFIG_ID);
        entity.setCushionMaxUseCount(Constants.CUSHION_DEFAULT_MAX_USE_CONT);
        mapper.insert(entity);
        return entity;
    }

    private OpcConfigDTO entity2DTO(OpcConfigEntity entity) {
        if (entity == null) return null;
        return BeanUtil.copyProperties(entity, OpcConfigDTO.class);
    }
}
