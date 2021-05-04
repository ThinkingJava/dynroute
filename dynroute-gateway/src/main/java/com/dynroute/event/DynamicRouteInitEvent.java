package com.dynroute.event;

import org.springframework.context.ApplicationEvent;

/**
 *
 * 路由初始化事件
 */
public class DynamicRouteInitEvent extends ApplicationEvent {

    public DynamicRouteInitEvent(Object source) {
        super(source);
    }

}
