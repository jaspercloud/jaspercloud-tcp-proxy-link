package io.jaspercloud.proxy.agent;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.util.concurrent.CountDownLatch;

@SpringBootApplication
public class AgentApplication {

    public static void main(String[] args) throws Exception {
        new SpringApplicationBuilder(AgentApplication.class).web(false).run(args);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }

}
