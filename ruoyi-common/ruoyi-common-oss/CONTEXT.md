# OSS 配置装配 (OSS Config) Context

本文件是 `ruoyi-common-oss` 中**配置装配子系统**的术语表(glossary),只收录 OSS 客户端如何获得配置这一条链路上的概念;模块里其它能力(OssClient 上传下载、分片、预签名等)不在此列。

> 背景:「OSS 配置回源 SPI」重构(ADR 0005/0006)正在进行,本表随设计决策同步更新。

## Language

**配置键 (configKey)**:
一条 OSS 配置的业务标识(如 minio、qiniu),`sys_oss_config` 表一行对应一个;OssClient 实例缓存、客户端锁、配置缓存哈希项均按它索引。
_Avoid_: ossId(那是文件对象的主键)、type/类型(易与 accessPolicy 等配置字段混)。

**默认配置键 (DEFAULT_CONFIG_KEY)**:
Redis 里的一个字符串键,存当前启用(status=YES)配置的 configKey;`OssFactory.instance()` 无参路径靠它定位默认客户端。
_Avoid_: 「默认配置」(它存的是键,不是配置体)。

**配置缓存 (sys_oss_config 哈希)**:
OSS 配置的运行时读源——Redis 哈希,项键为 configKey,载荷是序列化的 `SysOssConfig` **实体** JSON,读取方按字段子集解析为 `OssProperties`。现状缓存名无 `#ttl` 后缀 → 永不过期的 RMap;本重构同步改名 `sys_oss_config#30d`(项 30 天过期,`DEFAULT_CONFIG_KEY` 两个写点同加 30d),残留项由此获得兜底淘汰——「回源」词条里的「靠 TTL 到期淘汰」指的是改后状态。
_Avoid_: 把它当普通 cache-aside 缓存(miss 不会自动查库,自愈靠回源 SPI)。

**回源 (Reload)**:
配置缓存 miss 时经回源 SPI 从 DB **全量重建**配置缓存(哈希 + 默认配置键)的动作;与启动预热共用同一条写路径(`init()`)。重建只增写、不清理残留项(DB 已无而缓存尚存的项靠 TTL 到期淘汰,正常运维中由配置变更事件负责删除)。默认键有一处不对称:`init()` 仅在 DB 存在 status=YES 行时覆盖写默认键——只要还有启用行,回源顺带修复默认键;若 DB 已无任何启用行,旧默认键不更新也不删除,靠 30d TTL 到期,窗口内无参 `instance()` 是 stale hit、不触发 miss 自愈。
_Avoid_: refresh/刷新(易与配置变更事件的增量更新混)、懒加载(回源是全量重建,不是单键加载)、同步(回源不保证删除残留)。

**OssConfigProvider (配置回源 SPI)**:
common-oss 自持的单方法端口(`reload()`),由 ruoyi-system 实现;应用未提供实现 bean 时,miss 降级为抛异常(即回源能力可选)。
_Avoid_: OssConfigService(它不是业务服务,是能力模块的回源钩子)。

**配置变更事件 (OssConfigChangeEvent)**:
管理端增删改/启停配置后发布的事务提交后事件,是配置缓存的**主动更新**通道;回源 SPI 只是 miss 时的**修复**路径,二者并存不冲突。
_Avoid_: 把变更事件与回源混称「刷新缓存」。
