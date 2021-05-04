package com.dynroute.rule;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.List;

/**
 * 轮询算法
 */
@Slf4j
public class FullRoundGrayLoadBalancer implements GrayLoadBalancer {

    private DiscoveryClient discoveryClient;

    private volatile int index;

    public FullRoundGrayLoadBalancer(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    /**
     * 根据serviceId 筛选可用服务
     * @param serviceId 服务ID
     * @param request 当前请求
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
        if (index == instances.size()) {
            index = 0;
        }
        return instances.get(index++);
    }

}

