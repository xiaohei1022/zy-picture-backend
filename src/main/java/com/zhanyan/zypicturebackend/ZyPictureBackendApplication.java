package com.zhanyan.zypicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.zhanyan.zypicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class ZyPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZyPictureBackendApplication.class, args);
    }

}
