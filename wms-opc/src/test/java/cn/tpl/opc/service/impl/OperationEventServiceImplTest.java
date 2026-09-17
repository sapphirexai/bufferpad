package cn.tpl.opc.service.impl;

import cn.tpl.opc.commons.dto.enums.OperationEventCode;
import cn.tpl.opc.commons.dto.result.OperationEventDTO;
import cn.tpl.opc.entity.OperationEventEntity;
import cn.tpl.opc.mapper.OperationEventEntityMapper;
import cn.tpl.opc.service.ISseService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OperationEventServiceImplTest {
    private final OperationEventServiceImpl service = new OperationEventServiceImpl();
    private final OperationEventEntityMapper mapper = mock(OperationEventEntityMapper.class);
    private final ISseService sseService = mock(ISseService.class);

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(service, "operationEventEntityMapper", mapper);
        ReflectionTestUtils.setField(service, "sseService", sseService);
        ReflectionTestUtils.setField(service, "scanOperationLogs", mock(cn.tpl.opc.application.scan.ScanOperationLogService.class));
    }

    @Test
    public void deleteBeforeDeletesInBatchesUntilNoFullBatchRemains() {
        Date cutoff = new Date(123456789L);
        when(mapper.deleteBefore(eq(cutoff), eq(1000))).thenReturn(1000, 250);

        int deleted = service.deleteBefore(cutoff);

        Assert.assertEquals(1250, deleted);
        verify(mapper, org.mockito.Mockito.times(2)).deleteBefore(cutoff, 1000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteBeforeRejectsNullCutoff() {
        service.deleteBefore(null);
    }

    @Test
    public void publishPreservesOperationId() {
        OperationEventDTO event = OperationEventDTO.of(OperationEventCode.SCAN_COUNTED, 1);
        event.setOperationId("op-preserved");

        OperationEventDTO result = service.publish(event);

        Assert.assertEquals("op-preserved", result.getOperationId());
        org.mockito.ArgumentCaptor<OperationEventEntity> captor = org.mockito.ArgumentCaptor.forClass(OperationEventEntity.class);
        verify(mapper).insertSelective(captor.capture());
        Assert.assertEquals("op-preserved", captor.getValue().getOperationId());
    }

    @Test
    public void publishGeneratesOperationIdForLegacyEvents() {
        OperationEventDTO result = service.publish(OperationEventDTO.of(OperationEventCode.SCAN_COUNTED, 1));

        Assert.assertNotNull(result.getOperationId());
        Assert.assertEquals(result.getEventId(), result.getOperationId());
    }
}
