package cn.tpl.opc.infrastructure.plc;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PlcErrorClassifierTest {
    private final PlcErrorClassifier classifier = new PlcErrorClassifier();

    @Test
    public void classifiesHslConnectionMessageAsTransportFailure() {
        assertEquals(PlcFailureType.TRANSPORT,
                classifier.classify(10000, "Connection refused"));
    }

    @Test
    public void doesNotTreatHslAddressParsingFailureAsNetworkOutage() {
        assertEquals(PlcFailureType.DEVICE_REJECTED,
                classifier.classify(10000, "For input string: DB1,DBW0"));
    }

    @Test
    public void treatsEmptyHslConnectErrorAsTransportFailure() {
        assertEquals(PlcFailureType.TRANSPORT,
                classifier.classify(10000, null));
    }
}
