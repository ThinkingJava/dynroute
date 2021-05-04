package com.dynroute.configuration;

import com.dynroute.constant.CacheConstants;
import com.dynroute.route.RouteCacheHolder;
import com.dynroute.util.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.config.PropertiesRouteDefinitionLocator;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 动态路由配置类
 */
@Slf4j
@Configuration
@ComponentScan("com.dynroute")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class DynamicRouteAutoConfiguration {

    /**
     * 配置文件设置为空 redis 加载为准
     * @return
     */
    @Bean
    public PropertiesRouteDefinitionLocator propertiesRouteDefinitionLocator() {
        return new PropertiesRouteDefinitionLocator(new GatewayProperties());
    }

    /**
     * redis 监听配置
     * @param redisConnectionFactory redis 配置
     * @return
     */
    @Bean
    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory redisConnectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener((message, bytes) -> {
            log.warn("接收到重新JVM 重新加载路由事件");
            RouteCacheHolder.removeRouteList();
            // 发送刷新路由事件
            SpringContextHolder.publishEvent(new RefreshRoutesEvent(this));
        }, new ChannelTopic(CacheConstants.ROUTE_JVM_RELOAD_TOPIC));
        return container;
    }


}
