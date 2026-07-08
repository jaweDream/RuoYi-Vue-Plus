# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概览

RuoYi-Vue-Plus **6.X**（当前分支 `future/6.X-jar`）后台管理系统的后端，纯后端多模块 Maven 工程（前端为独立的 plus-ui 项目，不在本仓库）。技术栈：Spring Boot 4.0.6 + JDK 21 + MyBatis-Plus + dynamic-datasource + Sa-Token + Redisson + WarmFlow。Web 容器为 Jetty，序列化用 Jackson，主键为雪花 ID，逻辑删除全局开启。包根统一 `org.dromara`。

**6.X 已移除多租户**：无 `ruoyi-common-tenant` / `TenantHelper`、yml 无 tenant 配置；残留的 `tenant` 字样均与业务多租户无关（MP `@InterceptorIgnore(tenantLine=...)`、social 的 Azure AD `tenantId`、warm-flow 自带表的 `tenant_id` 列）。**横切上下文只剩数据权限与 Sa-Token 登录态**——谈 ThreadLocal/虚拟线程传播等问题时不要再提租户。

## 构建与运行

### JDK 与 Maven

**JDK 策略：高版本 JDK + `maven.compiler.release` 适配，不用 jenv、不必装/切 JDK 21。** pom `<java.version>21</java.version>` + `<release>${java.version}</release>` 让高版本 JDK 编译出 **release-21 字节码**，故在本机系统 JDK（当前 Homebrew OpenJDK 26 / IDEA JBR 25 均可）上直接 build/run 即可，**不要**再前置 `export JAVA_HOME=.../JBR`、`jenv` 或试图找 JDK 21。`mvnw` 无执行权限，统一用 `sh ./mvnw`。

- 首次把多模块装入本地仓库（reactor 模块未预装）：`sh ./mvnw -pl ruoyi-admin -am install -DskipTests` → 产出可执行 fat jar `ruoyi-admin/target/ruoyi-admin.jar`。
- 启动后端：IDEA 直接运行 `ruoyi-admin` 的 `org.dromara.DromaraApplication`；**或无头** `java -jar ruoyi-admin/target/ruoyi-admin.jar --spring.profiles.active=dev`（~8s，Jetty 监听 **:8080**）。改过 yml/上游源码后需先重 `install` 再 `java -jar`（jar 内嵌 yml）。Profile `local`/`dev`(默认)/`prod` 由 pom `profiles.active` 决定。
- 扩展服务各为独立 Spring Boot 应用，需单独运行各自 main 类：`ruoyi-monitor-admin`(`MonitorAdminApplication`，SpringBoot Admin 监控)、`ruoyi-snailjob-server`(`SnailJobServerApplication`，分布式任务调度中心)、`ruoyi-snailai-server`(`SnailAiServerApplication`)。
- 注：`.run/*.run.xml` 是 **Docker 镜像构建**配置（`type="docker-deploy"` + `buildOnly=true`，指向各模块 Dockerfile），**不是**应用启动配置。

### 本机数据源（当前分支实际跑在 PostgreSQL）

本机开发环境已从 MySQL 切到**本地 PostgreSQL** `ry-vue`（工作区改动：`application-dev.yml` master 数据源换 `org.postgresql.Driver` + `jdbc:postgresql://localhost:5432/ry-vue`、user `jarvey`、trust 空密码；`ruoyi-admin/pom.xml` 启用 `org.postgresql:postgresql` 驱动）。Redis 本机 `:6379`、密码 `ruoyi123`。

- 建库脚本用 **`script/sql/postgres/postgres_ry_*.sql`**（`_vue`/`_workflow`/`_ai`/`_chain`/`_job`）；MySQL 版在 `script/sql/*.sql`，另有 `{oracle,sqlserver}` 异构脚本与 `script/sql/update` 增量脚本。chain 的 postgres 版 `postgres_ry_chain.sql` 由 `ry_chain.sql` 移植（`bigint→int8`、`datetime→timestamp`、`decimal(65,0)→numeric(65,0)`、`sysdate()→now()`、内联 comment 外移为 `comment on`、`key→create [unique] index`、去掉 MySQL 前缀索引长度）。
- **db MCP**：项目根 `.mcp.json` 注册 `db-ry-vue`——用 `node@20` 跑本地 `universal-db-mcp` 连本地 PG `ry-vue`（`readwrite`）。MCP 在 **Claude Code 会话启动时加载**，新增/改动 `.mcp.json` 后须重载会话并批准该项目 MCP 才生效。

### 前端 plus-ui（独立仓库）

前端在 **`/Users/jarvey/IdeaProjects/plus-ui`**，是 `plus-ui-react`（UmiJS/max + **Vite 模式** + React 19，pnpm，`engines: node>=20`），**不在本仓库**。

- 启动：`cd /Users/jarvey/IdeaProjects/plus-ui`，本机默认 node 26 装原生依赖易翻车，用 **node@20** 更稳：`export PATH="/opt/homebrew/opt/node@20/bin:$PATH" && pnpm install && pnpm dev`。dev 端口 **8000**，代理 `/dev-api` → `http://localhost:8080`（见 `vite.config.ts`/`.env.development`）。默认账号 `admin / admin123`。
- **验证坑（易误判为故障）**：`umi dev` 实为 **Vite 模式**（`config.vite` 真值使 `enableVite=true`；日志里的 `[MFSU][eager] worker init` 是 max 默认 eager worker 的干扰项，**不是** webpack）。Vite 的 HTML 兜底中间件**只对带 `Accept: text/html` 的请求**返回首页——用 `curl http://localhost:8000/`（默认 `Accept: */*`）会得到 **404**，这是探测假象；加 `-H 'Accept: text/html'` 即 200，或直接用浏览器/打 `/dev-api/*` 代理验证。`pnpm build` 走 Vite/Rollup 产出 `dist/`。

## 测试

**完整规范见 `docs/测试规范.md`（改动测试基建须同步更新该文与本节）**。要点：

- 立场：业务系统**以全量 `@SpringBootTest` + 真实容器的集成测试为主**；纯单测仅用于无 Spring 依赖的纯函数/工具类。**禁止**用 `@MockBean` mock 掉 Mapper/拦截器去"测"数据权限——那恰好绕过了被测对象。
- 集成测试统一放 `ruoyi-admin/src/test`（全量上下文需聚合所有模块），包 `org.dromara.test.<领域>`，类名 `*IntegrationTest`，`extends TestcontainersBase`，三件套注解 `@Tag("dev")` + `@ActiveProfiles({"dev","test"})` + `@SpringBootTest`。需本机 **Docker daemon**（Testcontainers 起 MySQL 8.4 + Redis；首例见 `DataPermissionIntegrationTest`）。
- **`@Tag` / surefire `<groups>` 机制（必须理解）**：root pom `skipTests=true` 且 `<groups>=${profiles.active}`(默认 dev)。因此 ① 跑测试必须 `-DskipTests=false`；② **集成测试不加 `@Tag("dev")` 在 Maven 下不会执行**；③ `@Tag("exclude")` 永远被排除；④ IDEA 直接运行类会忽略 groups/skipTests（开发回路最顺）。
- 跑单个测试类（**不要加 `-am`**）：
  ```bash
  sh ./mvnw -pl ruoyi-admin test -DskipTests=false -Dtest=DataPermissionIntegrationTest
  ```
  `-pl ruoyi-admin test` **不带 `-am`** 时，上游兄弟模块从本地仓库（`~/.m2`）解析、**不会重新编译源码**。因此只要改动过上游（`ruoyi-common-*` / `ruoyi-modules/*` / `ruoyi-api`）源码，就必须**先**重跑 `sh ./mvnw -pl ruoyi-admin -am install -DskipTests` 刷新本地仓库 jar，**再**用上面的命令跑测试，否则是在旧 jar 上跑、可能假绿；仅当确信已装上游 jar 与工作区一致时才可省去这步重装。切勿用 `-am test` 一步到位：`-am` 会让上游模块也进入 `test` phase，而 `ruoyi-common-mybatis` 等上游 test classpath 无 JUnit 5 引擎，撞上 root pom 的 `<groups>` 配置直接硬失败（`groups/excludedGroups require ... JUnit 5`，无法用 surefire 参数绕过）。
- 数据权限/登录态测试：MOCK 不经 Sa-Token filter，须先 `SaTokenContextForThreadLocalStaff.setModelBox(new SaRequestForMock(), new SaResponseForMock(), new SaStorageForMock())`（`@AfterEach` `clearModelBox()`），再 `DataPermissionHelper.setVariable("user", loginUser)`，否则抛「SaTokenContext 上下文尚未初始化」。测试里 Mapper 用 `@Autowired` 字段注入（勿照搬业务代码的构造注入，否则 `No ParameterResolver registered for ... constructor`）。

## 模块结构

- **ruoyi-admin** — Web 入口（`DromaraApplication`），聚合业务模块依赖（`ruoyi-system`/`ruoyi-job`/`ruoyi-ai`/`ruoyi-demo`/`ruoyi-workflow`；`ruoyi-gen` 仅在 Maven `gen` profile 下引入，默认构建不含代码生成器）。`org.dromara.web` 下是登录认证：`AuthController`/`SysLoginService` + `IAuthStrategy` 多登录策略（password/sms/email/social/xcx）、验证码。集成测试也在此模块。
- **ruoyi-common** — 24 个能力 starter，按需被业务模块依赖；关键：`-core`(工具/基础 domain/异常)、`-mybatis`(BaseEntity/BaseMapperPlus/数据权限)、`-satoken`(登录鉴权)、`-redis`(Redisson/RedisUtils/缓存/Lock4j)、`-web`(BaseController/全局异常)、`-excel`、`-oss`、`-log`、`-json`、`-translation`、`-sensitive`、`-encrypt`、`-doc`(SpringDoc)、`-security`，以及 `-mail`/`-sms`/`-social`/`-mqtt`/`-elasticsearch`/`-ai`/`-mcp`/`-push`/`-job`；`-bom` 统一版本。
- **ruoyi-modules** — 业务模块：`ruoyi-system`(用户/角色/部门/菜单/字典/OSS/客户端等核心)、`ruoyi-workflow`(WarmFlow 审批)、`ruoyi-ai`(Spring AI)、`ruoyi-gen`(代码生成器)、`ruoyi-job`(SnailJob 客户端)、`ruoyi-demo`(功能案例)、`ruoyi-chain`(链资产，嵌套聚合模块：`-api` 链无关 SPI/模型 + `-evm`/`-solana`/`-aptos` 协议适配器 + `-biz` 业务编排层，设计见该模块 CONTEXT.md，SQL 为 `script/sql/ry_chain.sql`(MySQL) / `script/sql/postgres/postgres_ry_chain.sql`(PG))。注：`ruoyi-generator/` 仅为旧构建残留（无 pom/src），活动的生成器是 `ruoyi-gen`。
- **ruoyi-extend** — 独立部署的 server：`ruoyi-monitor-admin`、`ruoyi-snailjob-server`、`ruoyi-snailai-server`。
- **ruoyi-api** — 跨模块解耦的 API/模型层（`org.dromara.{system,workflow}.api` + `system/api/model`），供模块间调用而不直接依赖实现。

## CRUD 分层约定（业务模块内，以 ruoyi-system 为范本）

一个业务实体的标准链路：

- `domain/Xxx.java` — Entity，`extends BaseEntity`（自动填充 createDept/createBy/createTime/updateBy/updateTime），MP `@TableName`。
- `domain/bo/XxxBo.java` — 入参对象，带 Validation 校验注解。
- `domain/vo/XxxVo.java` — 出参对象，带 `@ExcelProperty`(导出)、`@Translation`(字典/翻译)、`@Sensitive`(脱敏) 等**序列化期**注解。
- `mapper/XxxMapper.java` — `extends BaseMapperPlus<Xxx, XxxVo>`（内置 VO 转换、批量等增强）。
- `service/IXxxService.java` + `service/impl/XxxServiceImpl.java`。
- `controller/.../XxxController.java` — `extends BaseController`，返回 `R<T>` / `R<PageResult<XxxVo>>`；分页用 `PageQuery` 入参；`@SaCheckPermission` 权限、`@Log` 操作日志、`@RepeatSubmit` 防重、`ExcelBuilder.of(...)` 导出。

Bo/Entity/Vo 间转换用 `MapstructUtils`（MapStruct Plus 编译期生成）。**接口文档基于 JavaDoc**（SpringDoc + therapi）——写规范的 Java 注释即可，不要堆 Swagger 注解。注释与 `@DisplayName` 用中文。

## 横切机制（"框架魔法"，bug 高发区）

- **数据权限**：Mapper 方法标 `@DataPermission({@DataColumn(key=..., value=...)})`，`PlusDataPermissionInterceptor` 按当前用户部门/角色（`DataScopeType`）无感重写 SQL；上下文走 `DataPermissionHelper`（普通 ThreadLocal）；超管短路。
- **认证授权**：Sa-Token，`LoginHelper` 取登录态，`@SaCheckPermission`/`@SaCheckRole`（支持 AND/OR 复杂表达式）。
- **MyBatis-Plus**：全局逻辑删除、雪花主键(`ASSIGN_ID`)、字段自动填充(`InjectionMetaObjectHandler`)、联表用 MyBatis-Plus-Join(MPJ)、p6spy 打印 SQL（测试中关闭）；`mapperPackage=org.dromara.**.mapper`、`typeAliasesPackage=org.dromara.**.domain`。
- **加密/脱敏/翻译**：`@EncryptField`(存取加解密)/`@ApiEncrypt`(接口传输加密) 经 MyBatis 拦截器与 Web 层；`@Sensitive`、`@Translation` 经 Jackson 序列化期处理。
- **缓存/锁/幂等**：Redisson + `RedisUtils`，扩展版 Spring-Cache 注解，Lock4j 分布式锁。
- **多数据源**：dynamic-datasource，`@DS` 切换；主从配置前缀 `spring.datasource.dynamic.datasource.*`。
