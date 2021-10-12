package io.jaspercloud.proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TcpProxyLinkServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TcpProxyLinkServerApplication.class, args);
    }

}
