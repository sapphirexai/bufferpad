package cn.tpl.opc.controller;

import cn.tpl.opc.service.INettyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/19
 */
@Tag(name = "设备", description = "设备相关接口")
@Slf4j
@RestController
@RequestMapping("/device")
public class DeviceController {
    @Resource
    private INettyService nettyService;


}