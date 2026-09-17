package cn.tpl.opc.service;

import cn.tpl.opc.commons.dto.result.OperationEventDTO;

import java.util.Date;
import java.util.List;

public interface IOperationEventService {
    OperationEventDTO publish(OperationEventDTO event);

    List<OperationEventDTO> listRecent(Integer workLine, Integer limit);

    int deleteBefore(Date cutoff);
}
