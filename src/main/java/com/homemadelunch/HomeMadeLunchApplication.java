package com.homemadelunch;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 家庭便当 - 工作日午餐沟通小程序
 */
@SpringBootApplication
@MapperScan("com.homemadelunch.mapper")
@EnableScheduling
public class HomeMadeLunchApplication {

    public static void main(String[] args) {
        SpringApplication.run(HomeMadeLunchApplication.class, args);
    }
}
