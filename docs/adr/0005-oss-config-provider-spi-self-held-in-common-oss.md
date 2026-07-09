# OSS 配置回源 SPI 自持在 common-oss 而非 ruoyi-api

`OssFactory`(common-oss)运行时以 Redis 为唯一配置读源:`DEFAULT_CONFIG_KEY` 或 `sys_oss_config` 哈希 miss 即抛异常,DB 里的 `sys_oss_config` 行救不了场(缓存写入只发生在启动 `init()` 与配置变更事件)。根因是依赖方向:common-oss 看不见 ruoyi-system 的 Mapper。决定新增回源 SPI `OssConfigProvider`,由 ruoyi-system 实现(查库 + 回填 Redis),`OssFactory` miss 时经接口回源自愈;无实现 bean 的应用(未引 ruoyi-system)降级为现状抛异常。

接口**定义在 `org.dromara.common.oss.spi`(common-oss 自持),而非照 ADR 0003 先例进 `ruoyi-api`**。两个场景的关键不对称:0003 当时 common-mybatis 本就依赖 ruoyi-api(零新增边),且 `IDataScopeService` 语义是 system 对外的业务查询;本例 common-oss 今天不依赖 ruoyi-api(照搬先例要新开一条「能力 starter → 业务 API 层」的边),且接口形状由 `OssFactory` 的两个 miss 点定义,语义是 OSS 能力的内部回源钩子——端口归消费者,而 ruoyi-system 已依赖 common-oss,自持是零新增边。判据沉淀为:**消费方已依赖 ruoyi-api 且接口是业务对外服务 → 进 ruoyi-api;接口由能力模块的需要定义、消费方不依赖 ruoyi-api → 能力模块自持**。
