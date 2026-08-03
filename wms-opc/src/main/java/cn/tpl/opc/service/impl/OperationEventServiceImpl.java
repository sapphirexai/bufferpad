package cn.tpl.opc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.tpl.opc.commons.dto.result.OperationEventDTO;
import cn.tpl.opc.entity.OperationEventEntity;
import cn.tpl.opc.mapper.OperationEventEntityMapper;
import cn.tpl.opc.service.IOperationEventService;
import cn.tpl.opc.service.ISseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OperationEventServiceImpl implements IOperationEventService {
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final int CLEANUP_BATCH_SIZE = 1000;

    @Resource
    private OperationEventEntityMapper operationEventEntityMapper;
    @Resource
    private ISseService sseService;

    @Override
    public OperationEventDTO publish(OperationEventDTO event) {
        if (event == null || event.getWorkLine() == null) return event;

        OperationEventDTO prepared = prepare(event);
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    persistAndSend(prepared);
                }
            });
        } else {
            persistAndSend(prepared);
        }
        return prepared;
    }

    @Override
    public List<OperationEventDTO> listRecent(Integer workLine, Integer limit) {
        int queryLimit = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));
        return operationEventEntityMapper.listRecent(workLine, queryLimit).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public int deleteBefore(Date cutoff) {
        if (cutoff == null) {
            throw new IllegalArgumentException("cutoff must not be null");
        }

        int totalDeleted = 0;
        int deleted;
        do {
            deleted = operationEventEntityMapper.deleteBefore(cutoff, CLEANUP_BATCH_SIZE);
            totalDeleted += deleted;
        } while (deleted == CLEANUP_BATCH_SIZE);
        return totalDeleted;
    }

    private OperationEventDTO prepare(OperationEventDTO source) {
        OperationEventDTO event = new OperationEventDTO();
        BeanUtil.copyProperties(source, event);
        if (event.getEventId() == null) event.setEventId(UUID.randomUUID().toString());
        if (event.getOperationId() == null || event.getOperationId().isBlank()) {
            event.setOperationId(event.getEventId());
        }
        if (event.getOccurredAt() == null) event.setOccurredAt(new Date());
        return event;
    }

    private void persistAndSend(OperationEventDTO event) {
        try {
            OperationEventEntity entity = new OperationEventEntity();
            BeanUtil.copyProperties(event, entity);
            entity.setCreatedDate(event.getOccurredAt());
            operationEventEntityMapper.insertSelective(entity);
            event.setId(entity.getId());
        } catch (Exception e) {
            log.error("persist operation event failed, eventId => {}", event.getEventId(), e);
        }
        sseService.sendOperationEvent(event);
    }

    private OperationEventDTO toDTO(OperationEventEntity entity) {
        OperationEventDTO dto = new OperationEventDTO();
        BeanUtil.copyProperties(entity, dto);
        dto.setOccurredAt(entity.getCreatedDate());
        return dto;
    }
}
