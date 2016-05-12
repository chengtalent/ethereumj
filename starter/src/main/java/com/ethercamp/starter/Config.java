package com.ethercamp.starter;

import com.ethercamp.starter.ethereum.EthereumBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

@Configuration
public class Config {

    @Bean
    EthereumBean ethereumBean() throws Exception {
        final EthereumBean ethereumBean = new EthereumBean();

        Executors.newSingleThreadExecutor().
                submit(new Runnable() {
                    @Override
                    public void run(){
                        ethereumBean.start();
                    }
                });

        return ethereumBean;
    }
}
