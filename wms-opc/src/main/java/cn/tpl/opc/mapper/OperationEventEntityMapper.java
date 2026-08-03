package cn.tpl.opc.mapper;

import cn.tpl.opc.entity.OperationEventEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface OperationEventEntityMapper {
    int insertSelective(OperationEventEntity entity);

    List<OperationEventEntity> listRecent(@Param("workLine") Integer workLine, @Param("limit") Integer limit);

    int deleteBefore(@Param("cutoff") Date cutoff, @Param("batchSize") Integer batchSize);
}
