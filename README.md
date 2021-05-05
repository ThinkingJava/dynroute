# 动态网关路由

## 1  前言

​	上个月入职了新公司，发现中台网关路由配置是放在apollo上的，所以这次目的是设计一款动态路由配置的网关。

## 2 设计

​	由于Gateway 的传统的通过配置中心及配置文件的方式配置路由略显得笨拙，通过api或者图形界面修改，则显得相对灵活的多。

### 2.1 技术选型

通过重写 spring cloud gateway 实现API动态更改路由，非常便捷的实现控制请求接入管理。路由信息使用mysql作为持久化存储，初始化启动加载，redis作为临时存储并发布订阅监听。

### 2.2 模块划分

将项目划分为以下几个模块

| 名称             | 描述                                                     |
| ---------------- | -------------------------------------------------------- |
| dynroute-api     | 一些公共的代码，常量，异常类等。                         |
| dynroute-core    | 测试应用功能模块                                         |
| dynroute-gateway | 网关应用，消费mq投递消息，消息鉴权，负载均衡，路由转发等 |
| dynroute-server  | 测试应用功能模块                                         |

## 3 重构网关说明
### 3.1 网关接入数据库加载路由信息

- 创建数据库表用于保存路由配置

  ```sql
   CREATE TABLE `gateway_route_config` (
    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `route_name` varchar(30) DEFAULT NULL,
    `route_id` varchar(30) DEFAULT NULL,
    `predicates` json DEFAULT NULL COMMENT '断言',
    `filters` json DEFAULT NULL COMMENT '过滤器',
    `uri` varchar(50) DEFAULT NULL,
    `order` int(2) DEFAULT '0' COMMENT '排序',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `del_flag` char(1) DEFAULT '0',
    PRIMARY KEY (`id`) USING BTREE
  ) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='路由配置表';
  ```
  
- 初始化网关配置为空

  ```java
      /**
       * 配置文件设置为空 redis 加载为准
       * @return
       */
      @Bean
      public PropertiesRouteDefinitionLocator propertiesRouteDefinitionLocator() {
          return new PropertiesRouteDefinitionLocator(new GatewayProperties());
      }
  ```
  
- 初始化路由配置
  
  ```java
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
  ```
  
  
  
- 设置监听路由配置，并监听订阅
  
```java
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
  ```
  
- 路由配置查询及修改
  
```java
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
  ```



## 4 本地调试

### 4.1 本地启动nacos服务。

从 Github 上下载源码方式

```bash
git clone https://github.com/alibaba/nacos.git
cd nacos/
mvn -Prelease-nacos -Dmaven.test.skip=true clean install -U  
ls -al distribution/target/

// change the $version to your actual path
cd distribution/target/nacos-server-$version/nacos/bin
//启动nacos
startup.cmd -m standalone
```

### 4.2 Redis和Mysql



### 4.3 往mysql插入路由配置

```sql
INSERT  INTO `gateway_route_config`(`id`,`route_name`,`route_id`,`predicates`,`filters`,`uri`,`order`,`create_time`,`update_time`,`del_flag`) VALUES 
(1,'动态网关测试','dynroute-server','[{\"args\": {\"_genkey_0\": \"/server/**\"}, \"name\": \"Path\"}]','[]','lb://dynroute-server',0,'2021-05-04 16:44:41','2021-05-05 04:48:51','0');
```



### 4.2 启动网关Gateway

实例1配置： 在启动参数VM options 添加 -Dserver.port=9999

### 4.3 启动dynroute-core

### 4.5 启动dynroute-server

### 4.6 postman调试

这个时候服务core服务是不通的。

![image](D:image\image1.png)

新增网关路由配置

![](D:image\image2.png)

```json
[
    {
        "routeId": "dynroute-server",
        "routeName": "动态网关测试",
        "predicates": [
            {
                "args": {
                    "_genkey_0": "/server/**"
                },
                "name": "Path"
            }
        ],
        "filters": [],
        "uri": "lb://dynroute-server",
        "order": 1,
        "delFlag": "0"
    },
    {
        "routeId": "dynroute-core",
        "routeName": "动态网关测试",
        "predicates": [
            {
                "args": {
                    "_genkey_0": "/core/**"
                },
                "name": "Path"
            }
        ],
        "filters": [],
        "uri": "lb://dynroute-core",
        "order": 2,
        "delFlag": "0"
    }
]
```

新增路由配置后，则可访问

![](D:image\image3.png)







