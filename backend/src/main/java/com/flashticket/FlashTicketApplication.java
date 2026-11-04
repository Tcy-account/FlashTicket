package com.flashticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * FlashTicket 高并发票务抢购平台 · 模块化单体后端启动类。
 *
 * <p>架构约束（见 doc/跨域一致性与工程落地审查.md 6.1）：
 * 单 Spring Boot 应用、单 MySQL 业务库，按业务域组织包结构，
 * 业务模块之间禁止跨域直调 Mapper，统一走「编排服务 → 领域服务接口 → 数据访问层」。
 */
@SpringBootApplication
@EnableScheduling
public class FlashTicketApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashTicketApplication.class, args);
    }
}
