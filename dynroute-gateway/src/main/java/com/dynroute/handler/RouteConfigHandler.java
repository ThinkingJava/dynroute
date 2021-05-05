package com.dynroute.handler;

import cn.hutool.json.JSONArray;
import com.dynroute.service.GatewayRouteConfigService;
import com.dynroute.util.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * 路由信息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RouteConfigHandler implements HandlerFunction<ServerResponse> {

    private final GatewayRouteConfigService gatewayRouteConfigService;
    private final ObjectMapper objectMapper;

    /**
     * 路由配置查询及修改
     * @param serverRequest
     * @return
     */
    @SneakyThrows
    @Override
    public Mono<ServerResponse> handle(ServerRequest serverRequest) {
        HttpMethod method = serverRequest.method();
        if (method == HttpMethod.GET) {
            return ServerResponse.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(objectMapper.writeValueAsString(Response.ok(gatewayRouteConfigService.list()))));
        } else if (method == HttpMethod.PUT) {
            Mono<Response> routeStatus = serverRequest.bodyToMono(JSONArray.class).flatMap(updateRoutes());
            BodyInserter bodyInserter = BodyInserters.fromPublisher(routeStatus, Response.class);
            return ServerResponse.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(bodyInserter);
        }
        return ServerResponse.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(objectMapper.writeValueAsString(Response.failed())));
    }

    private Function updateRoutes() {
        return data -> {
            return Mono.just(Response.ok(gatewayRouteConfigService.updateRoutes((JSONArray) data)));
        };
    }
}
