package com.dynroute.constant;

public interface CacheConstants {

    String PREFIX = "dynroute:";

    /**
     * 路由存放
     */
    String ROUTE_KEY = PREFIX + "gateway_dynamic_route_key";

    /**
     * 内存reload 时间
     */
    String ROUTE_JVM_RELOAD_TOPIC = PREFIX + "gateway_jvm_route_reload_topic";

}
