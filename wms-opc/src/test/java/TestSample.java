import cn.tpl.opc.OpcApplication;
import cn.tpl.opc.netty.MsgBus;
import com.alibaba.druid.sql.visitor.functions.Char;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/3/31
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = OpcApplication.class)
public class TestSample {
    @Resource
    private MsgBus nettyMsgBus;

    @Test
    public void testSample() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.sss");
        Date a = sdf.parse("2023-04-17 17:03:51.530000");
        Date b = sdf.parse("2023-04-17 18:03:51.530000");
        System.out.println(b.getTime() - a.getTime());
        System.out.println(60 * 60 * 1000);
        System.out.println("测试样本方法");
    }

    @Test
    public void testNettyMsg() {
        nettyMsgBus.sendMsg("192.0.2.5", 4001, "A msg from Gavin`s NettyMsgBus.");
    }
}
;