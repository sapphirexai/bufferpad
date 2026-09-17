import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Small utility test. Hardware communication is covered by mocked flow tests.
 */
public class TestSample {
    @Test
    public void oneHourHasExpectedMilliseconds() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date start = format.parse("2023-04-17 17:03:51.530");
        Date end = format.parse("2023-04-17 18:03:51.530");
        Assert.assertEquals(60L * 60L * 1000L, end.getTime() - start.getTime());
    }
}
