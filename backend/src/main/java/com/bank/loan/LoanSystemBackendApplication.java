package com.bank.loan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringBootApplication
@EnableWebSecurity
@MapperScan("com.bank.loan.mapper")
public class LoanSystemBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoanSystemBackendApplication.class, args);
    }
}
