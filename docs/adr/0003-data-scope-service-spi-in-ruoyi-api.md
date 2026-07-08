# 数据范围服务(sdss)以 SPI 形式发布到 ruoyi-api

policy 计算**模板引用 `@sdss` 的 scope**(自定义、部门及以下、部门及以下或本人)时,需调 `sdss`(`ISysDataScopeService`)查库把范围展开成部门 id 串。但 policy 所在的 `ruoyi-common-mybatis` **不能依赖业务模块 `ruoyi-system`**(`ISysDataScopeService` 所在处)——这正是现有代码用 SpEL bean 字符串 `@sdss` 做晚绑定的原因。

决定:把该服务的 2 个方法接口提升到 **`ruoyi-api`**(新增 `IDataScopeService`,`common-mybatis` 已依赖该解耦层),让 `ISysDataScopeService extends IDataScopeService`,policy 构造期注入**类型化的** `IDataScopeService`(生产 bean 仍 `@Service("sdss")`,policy 内部以 `@sdss` 喂给 SpEL、模板不改)。

相比注入 `BeanResolver`(只动一个模块、但依赖仍是字符串名),类型化 SPI 让 policy 生产环境也脱离静态 Spring 容器,并能用干净的 typed fake 做纯单测。代价是多动 `ruoyi-api` + `ruoyi-system` 两处(各一处极小改动)。
