package com.dynroute;

import com.dynroute.annotation.EnableDynamicRoute;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDynamicRoute
@EnableDiscoveryClient
@SpringBootApplication
public class DynRouteGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(DynRouteGatewayApplication.class, args);
    }

}
