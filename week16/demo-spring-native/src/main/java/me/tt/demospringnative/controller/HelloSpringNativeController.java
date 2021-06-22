package me.tt.demospringnative.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloSpringNativeController {
    @RequestMapping("/hello")
    public String helloWorld(){
        return "hello, Spring Native!";
    }
}
