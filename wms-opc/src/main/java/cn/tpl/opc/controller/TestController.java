package cn.tpl.opc.controller;

import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.service.INettyService;
import com.alibaba.druid.stat.DruidStatManagerFacade;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4 /3
 */
@Tag(name = "测试", description = "存放一些业务不相关的测试接口")
@Slf4j
@RestController
@RequestMapping("/test")
public class TestController {

    @Resource
    private INettyService nettyService;

    @GetMapping("/testStr")
    public String testStr() {
        return "testStr";
    }

    @GetMapping("/druidStat")
    public Object druidStat() {
        // DruidStatManagerFacade#getDataSourceStatDataList 该方法可以获取所有数据源的监控数据，除此之外 DruidStatManagerFacade 还提供了一些其他方法，你可以按需选择使用。
        return DruidStatManagerFacade.getInstance().getDataSourceStatDataList();
    }

    @GetMapping("/plcMsgResult")
    public ResultDTO<Boolean> plcMsgResult(String ip, Integer port, String msg) {
        if (StringUtils.isEmpty(ip)) return ResultDTO.failure("IP地址不能为空！");
        if (null == port) return ResultDTO.failure("端口号不能为空！");
        return nettyService.sendMsg(ip, port, msg);
    }

    @GetMapping("/scannerConnections")
    public ResultDTO<Boolean> scannerConnections() {
        return nettyService.connectScanner();
    }
}
