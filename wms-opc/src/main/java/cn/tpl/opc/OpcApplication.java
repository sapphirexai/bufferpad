package cn.tpl.opc;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/3/30
 */
@OpenAPIDefinition(servers = {@Server(url = "/", description = "默认当前域名地址为服务地址")})
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class OpcApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpcApplication.class, args);
    }
}
