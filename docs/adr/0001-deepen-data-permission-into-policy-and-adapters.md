# 在 @DataPermission 背后把数据权限拆成 policy + 三个适配器

数据权限原本把「上下文获取 + 角色策略 + scope→SQL 文本 + SQL-AST 改写」四件事挤在 `PlusDataPermissionHandler` 一个方法里——注解看着小,背后却是很宽的隐藏运行时接口(shallow module)。issue #1 决定在**保留 `@DataPermission` 声明与 mapper 写法不变**的前提下,把它拆成四层:

- **声明适配器** = `@DataPermission` + `DataPermissionAdvice`(捕获声明,不变);
- **策略核心** = `DataPermissionPolicy`,纯函数 `(声明, AuthorizationContext, OperationKind) → DataFilter`;
- **MyBatis 适配器** = 变薄的 `PlusDataPermissionInterceptor`,只做 SQL-AST 挂载;
- **上下文适配器** = context provider,组装不可变快照。

目标是 locality 与可测性:多数规则改动落在 policy、多数规则测试只穿 policy 一个 seam。本次为 behavior-preserving 重构,任何语义变更另开工单。
