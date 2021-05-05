package com.dynroute.configuration;

import com.dynroute.handler.RouteConfigHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;

/**
 * 动态路由配置入口
 */
@Slf4j
@Configuration
@AllArgsConstructor
public class RouterFunctionConfiguration {

    private final RouteConfigHandler routeMessageHandler;

    @Bean
    public RouterFunction routerFunction() {
        return RouterFunctions
                .route(RequestPredicates.path("/route/**").and(RequestPredicates.accept(MediaType.ALL)),
                        routeMessageHandler);

    }

}
