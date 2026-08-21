package cn.tpl.opc.commons.dto.enums;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeviceTypeEnumTest {
    @Test
    public void recognizesBothSiemensS7ModelsAsPlcs() {
        assertTrue(DeviceTypeEnum.isPlc(3));
        assertTrue(DeviceTypeEnum.isPlc(4));
        assertTrue(DeviceTypeEnum.isSiemensS7(3));
        assertTrue(DeviceTypeEnum.isSiemensS7(4));
        assertFalse(DeviceTypeEnum.isSiemensS7(1));
        assertEquals("西门子 S7-1200 PLC", DeviceTypeEnum.of(3).getLabel());
        assertEquals("西门子 S7-1500 PLC", DeviceTypeEnum.of(4).getLabel());
    }
}
