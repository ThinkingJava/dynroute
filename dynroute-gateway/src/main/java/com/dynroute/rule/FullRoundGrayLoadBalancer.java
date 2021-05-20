package com.dynroute.rule;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询算法
 */
@Slf4j
public class FullRoundGrayLoadBalancer implements GrayLoadBalancer {

    private DiscoveryClient discoveryClient;

    private AtomicInteger position;

    public FullRoundGrayLoadBalancer(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        this.position = new AtomicInteger((new Random()).nextInt(100));
    }

    /**
     * 根据serviceId 筛选可用服务
     *
     * @param serviceId 服务ID
     * @param request   当前请求
     * @return
     */
    @Override
    public ServiceInstance choose(String serviceId, ServerHttpRequest request) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);

        // 注册中心无实例 抛出异常
        if (CollUtil.isEmpty(instances)) {
            log.warn("No instance available for {}", serviceId);
            throw new NotFoundException("No instance available for " + serviceId);
        }
        int pos = Math.abs(this.position.incrementAndGet());
        return instances.get(pos % instances.size());
    }

}

