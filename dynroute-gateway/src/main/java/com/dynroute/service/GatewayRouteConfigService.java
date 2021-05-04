package com.dynroute.service;

import cn.hutool.json.JSONArray;
import com.baomidou.mybatisplus.extension.service.IService;
import com.dynroute.entity.GatewayRouteConfig;

public interface GatewayRouteConfigService extends IService<GatewayRouteConfig> {

    /**
     * 更新路由信息
     * @param routes 路由信息
     * @return
     */
    Boolean updateRoutes(JSONArray routes);

}
