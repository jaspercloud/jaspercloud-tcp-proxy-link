package io.jaspercloud.proxy.controller;

import io.jaspercloud.proxy.support.socks5.Socks5Control;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/socks5")
public class Socks5Controller {

    @Autowired
    private Socks5Control socks5Control;

    @GetMapping("/cleanMaxRetry")
    public void cleanMaxRetry() {
        socks5Control.cleanMaxRetry();
    }
}
