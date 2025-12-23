package com.example.codefather;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class CodeFatherApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeFatherApplication.class, args);
    }

}
