import HslCommunication.Core.Types.OperateResult;
import HslCommunication.ModBus.ModbusTcpNet;
import cn.tpl.opc.OpcApplication;
import cn.tpl.opc.netty.MsgBus;
import cn.tpl.opc.util.FastJsonUtils;
import com.alibaba.druid.sql.visitor.functions.Char;
import lombok.extern.slf4j.Slf4j;
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
        nettyMsgBus.sendMsg("localhost", 6000, "A msg from Gavin`s NettyMsgBus.");
    }

    @Test
    public void testPLC() {
        ModbusTcpNet modbusTcpNet = new ModbusTcpNet("localhost", 6000, (byte) 1);
        modbusTcpNet.ConnectServer();
        short D9001 = modbusTcpNet.ReadInt16("D9001").Content;
        System.out.println("PLC Result: => " + D9001);
    }
}
