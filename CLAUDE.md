# CLAUDE.md

本文件指导 Claude Code（claude.ai/code）在本仓库工作。

## 项目概览

RuoYi-Vue-Plus **6.X** 后端（分支 `future/6.X-jar`）：纯后端多模块 Maven 工程，前端为独立的 plus-ui（不在本仓库）。Spring Boot 4.0.6 + JDK 21 + MyBatis-Plus + dynamic-datasource + Sa-Token + Redisson + WarmFlow；Jetty 容器、Jackson 序列化、雪花 ID、全局逻辑删除，包根 `org.dromara`。

**6.X 已移除多租户**：无 `ruoyi-common-tenant`/`TenantHelper`、yml 无 tenant 配置；残留的 `tenant` 字样都与业务无关（MP `@InterceptorIgnore(tenantLine=...)`、social 的 Azure AD `tenantId`、warm-flow 表的 `tenant_id` 列）。横切上下文只剩**数据权限 + Sa-Token 登录态**——谈 ThreadLocal/虚拟线程传播时别再提租户。

## 构建与运行

> JDK 策略、本机 PG 数据源与建库/迁移脚本、前端启动与 Vite 探测坑 → `docs/本机环境与构建.md`。

- **构建**：统一 `sh ./mvnw`（mvnw 无执行权限）；高版本 JDK 直接 build/run，别切 JDK21/jenv/设 JAVA_HOME。首次装本地仓库：`sh ./mvnw -pl ruoyi-admin -am install -DskipTests` → fat jar `ruoyi-admin/target/ruoyi-admin.jar`；改过 yml/上游源码要先重 `install`。
- **启动后端**：`java -jar ruoyi-admin/target/ruoyi-admin.jar --spring.profiles.active=dev`（~8s，Jetty **:8080**），或 IDEA 跑 `org.dromara.DromaraApplication`。
- **启动前端**（详见上述 doc）：plus-ui dev **:8000**，代理 `/dev-api`→:8080，账号 `admin/admin123`。
- **本机数据源**：本地 PostgreSQL `ry-vue`（:5432，user `jarvey`，空密码）；Redis `:6379` 密码 `ruoyi123`。
- **Claude 代跑服务一律后台运行**（Bash `run_in_background: true`），别前台阻塞、别 `nohup &` detach 出会话——进程挂到 CLI 后台任务，日志实时可见、`/bashes` 可查可停。关服务按端口 kill（:8080 后端 / :8000 前端），JVM 收尾慢可补 `kill -9`。用户想自控可用 `! <命令>` 前缀在会话 shell 直接跑。

## 测试

> 完整规范、Testcontainers 基建、所有踩坑与 Sa-Token mock 模板 → `docs/测试规范.md`。要点：

- **以全量 `@SpringBootTest` + 真实容器的集成测试为主**；纯单测只给无 Spring 依赖的工具类。禁止用 `@MockBean` mock 掉 Mapper/拦截器去“测”数据权限（那正好绕过被测对象）。
- 集成测试放 `ruoyi-admin/src/test`，包 `org.dromara.test.<领域>`，类名 `*IntegrationTest`，`extends TestcontainersBase`，注解三件套 `@Tag("dev")` + `@ActiveProfiles({"dev","test"})` + `@SpringBootTest`；需本机 Docker daemon。
- 跑单类（**不加 `-am`**）：`sh ./mvnw -pl ruoyi-admin test -DskipTests=false -Dtest=Xxx`。硬性前提：必须 `-DskipTests=false`；不加 `@Tag("dev")` Maven 不执行；改过上游源码先 `-am install` 刷新本地仓库再跑，否则旧 jar 假绿。「不加 `-am`」仅当本地仓库已**全量** install 过（含 `ruoyi-gen`，gen profile 默认激活即编译期依赖）时才成立——否则单模块反应堆解析不到 `ruoyi-gen`，在依赖解析阶段就 BUILD FAILURE（不是假绿，是根本没跑起来），须先补一次 `sh ./mvnw -pl ruoyi-admin -am install -DskipTests`。IDEA 直接跑类忽略 groups/skipTests，开发回路最顺。

## 模块结构

- **ruoyi-admin** — Web 入口（`DromaraApplication`），聚合业务模块（`ruoyi-system`/`ruoyi-job`/`ruoyi-ai`/`ruoyi-demo`/`ruoyi-workflow`；`ruoyi-gen` 经 `gen` profile 引入，但该 profile `activeByDefault=true` 默认即激活，故 admin 默认就编译期依赖它——不是"手动开 gen 才有"）。`org.dromara.web` 是登录认证：`AuthController`/`SysLoginService` + `IAuthStrategy` 多策略（password/sms/email/social/xcx）。集成测试也在此模块。
- **ruoyi-common** — 24 个能力 starter，按需依赖；关键：`-core`(工具/基础 domain/异常)、`-mybatis`(BaseEntity/BaseMapperPlus/数据权限)、`-satoken`、`-redis`(Redisson/RedisUtils/Lock4j)、`-web`(BaseController/全局异常)、`-excel`/`-oss`/`-log`/`-json`/`-translation`/`-sensitive`/`-encrypt`/`-doc`/`-security`，及 `-mail`/`-sms`/`-social`/`-mqtt`/`-elasticsearch`/`-ai`/`-mcp`/`-push`/`-job`；`-bom` 统一版本。
- **ruoyi-modules** — 业务模块：`ruoyi-system`(用户/角色/部门/菜单/字典/OSS/客户端等核心)、`ruoyi-workflow`(WarmFlow)、`ruoyi-ai`(Spring AI)、`ruoyi-gen`(代码生成器)、`ruoyi-job`(SnailJob 客户端)、`ruoyi-demo`、`ruoyi-chain`(链资产，嵌套聚合：`-api` SPI/模型 + `-evm`/`-solana`/`-aptos` 适配器 + `-biz` 编排；设计见其 CONTEXT.md，SQL `script/sql/ry_chain.sql` / `postgres/postgres_ry_chain.sql`)。注：`ruoyi-generator/` 是旧残留，活动生成器是 `ruoyi-gen`。
- **ruoyi-extend** — 独立部署 server：`ruoyi-monitor-admin`、`ruoyi-snailjob-server`、`ruoyi-snailai-server`。
- **ruoyi-api** — 跨模块解耦的 API/模型层（`org.dromara.{system,workflow}.api` + `system/api/model`），供模块间调用而不依赖实现。

## CRUD 分层约定（以 ruoyi-system 为范本）

`domain/Xxx`（Entity，`extends BaseEntity` 自动填充 createDept/By/Time 等，`@TableName`）→ `domain/bo/XxxBo`（入参，Validation 校验）→ `domain/vo/XxxVo`（出参，`@ExcelProperty`/`@Translation`/`@Sensitive` 等序列化期注解）→ `mapper/XxxMapper`（`extends BaseMapperPlus<Xxx,XxxVo>`）→ `service/IXxxService` + `impl/XxxServiceImpl` → `controller/XxxController`（`extends BaseController`，返回 `R<T>`/`R<PageResult<XxxVo>>`，分页用 `PageQuery`，`@SaCheckPermission`/`@Log`/`@RepeatSubmit`/`ExcelBuilder.of(...)`）。

Bo/Entity/Vo 转换用 `MapstructUtils`（编译期生成）。接口文档基于 JavaDoc（SpringDoc + therapi），写规范 Java 注释即可、别堆 Swagger 注解；注释与 `@DisplayName` 用中文。

## 横切机制（“框架魔法”，bug 高发区）

- **数据权限**：Mapper 标 `@DataPermission({@DataColumn(key=,value=)})`，`PlusDataPermissionInterceptor` 按用户部门/角色（`DataScopeType`）重写 SQL；上下文走 `DataPermissionHelper`（普通 ThreadLocal）；超管短路。
- **认证授权**：Sa-Token，`LoginHelper` 取登录态，`@SaCheckPermission`/`@SaCheckRole`（支持 AND/OR）。
- **MyBatis-Plus**：全局逻辑删除、雪花主键(`ASSIGN_ID`)、字段自动填充(`InjectionMetaObjectHandler`)、联表用 MPJ、p6spy 打印 SQL（测试关闭）；`mapperPackage=org.dromara.**.mapper`、`typeAliasesPackage=org.dromara.**.domain`。
- **加密/脱敏/翻译**：`@EncryptField`(存取)/`@ApiEncrypt`(传输) 经 MyBatis 拦截器与 Web 层；`@Sensitive`/`@Translation` 经 Jackson 序列化期。
- **缓存/锁/幂等**：Redisson + `RedisUtils`，扩展版 Spring-Cache 注解，Lock4j 分布式锁。
- **多数据源**：dynamic-datasource，`@DS` 切换；前缀 `spring.datasource.dynamic.datasource.*`。
