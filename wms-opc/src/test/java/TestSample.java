import HslCommunication.Core.Types.OperateResult;
import HslCommunication.Core.Types.OperateResultExOne;
import HslCommunication.Profinet.Melsec.MelsecMcNet;
import cn.tpl.opc.OpcApplication;
import cn.tpl.opc.service.INettyService;
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
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = OpcApplication.class)
public class TestSample implements AutoCloseable {
    @Resource
    private INettyService nettyService;

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
    public void testPLC() {
        MelsecMcNet melsecMc = new MelsecMcNet("127.0.0.1", 6000);
        melsecMc.ConnectServer();
        OperateResult connectResult = melsecMc.ConnectServer();
        if (connectResult.IsSuccess) {
            System.out.println("连接成功");
            OperateResultExOne<Short> D7000 = melsecMc.ReadInt16("D7000");
            if (D7000.IsSuccess) {
                System.out.println("读取结果：" + D7000.Content);
            } else {
                System.out.println("读取失败：" + D7000.Message);
            }
        } else {
            System.out.print("连接失败：" + connectResult.Message);
        }
    }


    @Override
    public void close() throws Exception {
        System.out.println("AutoClose");
    }
}
