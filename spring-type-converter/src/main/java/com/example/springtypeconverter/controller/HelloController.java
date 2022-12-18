package com.example.springtypeconverter.controller;

import com.example.springtypeconverter.type.IpPort;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class HelloController {

    @GetMapping("/hello-v1")
    public String helloV1(HttpServletRequest request) {
        // 문자 타입 조회
        String data = request.getParameter("data");

        // 숫자 타입으로 변경
        Integer intValue = Integer.valueOf(data);

        log.info("intValue = {}", intValue);

        return "ok";
    }

    /**
     * @RequestParam을 사용하면
     * 문자 10을 Integer 타입의 숫자로 스프링이 중간에 변환해준다.
     */
    @GetMapping("/hello-v2")
    public String helloV2(@RequestParam Integer data) {
        log.info("intValue = {}", data);

        return "ok";
    }

    @GetMapping("/ip-port")
    public String ipPort(@RequestParam IpPort ipPort) {
        log.info("ipPort IP = {}", ipPort.getIp());
        log.info("ipPort PORT = {}", ipPort.getPort());
        return "ok";
    }
}
