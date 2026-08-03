package cn.tpl.opc.service.impl;

import cn.tpl.opc.mapper.OperationEventEntityMapper;
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

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(service, "operationEventEntityMapper", mapper);
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
}
