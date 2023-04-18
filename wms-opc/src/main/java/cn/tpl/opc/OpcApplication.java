package cn.tpl.opc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/3/30
 */
@SpringBootApplication
@EnableDiscoveryClient
public class OpcApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpcApplication.class, args);
    }
}
