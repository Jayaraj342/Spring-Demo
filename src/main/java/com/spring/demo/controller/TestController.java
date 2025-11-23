package com.spring.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class TestController {

    @Value("${dynamic.hello:defaultDynamicHello}")
    private String helloMessage;

    @GetMapping("/test")
    public String test() {
        return helloMessage;
    }
}
