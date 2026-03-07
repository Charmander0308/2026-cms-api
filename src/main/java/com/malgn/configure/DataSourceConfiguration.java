package com.malgn.configure;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

// HikariDataSource 는 첫 getConnection() 시 지연 초기화되므로,
// 풀 시작 전에 ThreadFactory 를 교체하기 위해 BeanPostProcessor 를 사용한다.
@Configuration
public class DataSourceConfiguration implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof HikariDataSource hikari) {
            hikari.setThreadFactory(
                    Thread.ofVirtual()
                          .name("hikari-vt-", 0)
                          .factory()
            );
        }
        return bean;
    }
}
