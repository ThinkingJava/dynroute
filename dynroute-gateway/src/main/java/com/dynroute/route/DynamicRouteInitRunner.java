package com.dynroute.route;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.dynroute.constant.CacheConstants;
import com.dynroute.event.DynamicRouteInitEvent;
import com.dynroute.service.GatewayRouteConfigService;
import com.dynroute.vo.RouteDefinitionVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.scheduling.annotation.Async;

import java.net.URI;

@Slf4j
@Configuration
@AllArgsConstructor
public class DynamicRouteInitRunner {

    private final RedisTemplate redisTemplate;

    private final GatewayRouteConfigService gatewayRouteConfigService;

    @Async
    @Order
    @EventListener({WebServerInitializedEvent.class, DynamicRouteInitEvent.class})
    public void initRoute() {
        redisTemplate.delete(CacheConstants.ROUTE_KEY);
        log.info("开始初始化网关路由");

        gatewayRouteConfigService.list().forEach(route -> {
            RouteDefinitionVo vo = new RouteDefinitionVo();
            vo.setRouteName(route.getRouteName());
            vo.setId(route.getRouteId());
            vo.setUri(URI.create(route.getUri()));
            vo.setOrder(route.getOrder());

            JSONArray filterObj = JSONUtil.parseArray(route.getFilters());
            vo.setFilters(filterObj.toList(FilterDefinition.class));
            JSONArray predicateObj = JSONUtil.parseArray(route.getPredicates());
            vo.setPredicates(predicateObj.toList(PredicateDefinition.class));

            log.info("加载路由ID：{},{}", route.getRouteId(), vo);
            redisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(RouteDefinitionVo.class));
            redisTemplate.opsForHash().put(CacheConstants.ROUTE_KEY, route.getRouteId(), vo);
        });

        // 通知网关重置路由
        redisTemplate.convertAndSend(CacheConstants.ROUTE_JVM_RELOAD_TOPIC, "路由信息,网关缓存更新");
        log.debug("初始化网关路由结束 ");
    }

}
