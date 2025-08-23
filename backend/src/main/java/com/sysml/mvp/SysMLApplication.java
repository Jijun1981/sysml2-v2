package com.sysml.mvp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SysML v2 MVP应用主类
 */
@SpringBootApplication
@EnableScheduling
public class SysMLApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(SysMLApplication.class, args);
    }
}