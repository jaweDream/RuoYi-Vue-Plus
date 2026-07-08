# 数据权限 (Data Permission) Context

本文件是 `ruoyi-common-mybatis` 中**数据权限子系统**的术语表(glossary),只收录数据权限相关、且为本项目特有的概念。模块里其它能力(BaseEntity、BaseMapperPlus、自动填充等)不在此列。

> 背景:issue #1「Deepen DataPermission policy module」的深化重构正在进行,本表随设计决策同步更新。

## Language

**数据权限 (Data Permission)**:
行级数据访问控制——按当前登录用户的部门/角色,在 Mapper 执行的 SQL 上无感追加过滤条件,使其只能访问被授权的数据行。声明入口是 Mapper 方法上的 `@DataPermission`。
_Avoid_: 「权限」(太泛,易与功能权限 / 菜单权限 / `@SaCheckPermission` 混)、data scope(那只是其中一个维度)。

**Data Scope (数据范围)**:
单个角色被授予的可见范围**类型**,枚举 `DataScopeType` 的一个取值:全部 / 自定义 / 本部门 / 本部门及下级 / 仅本人 / 本部门及下级或本人。一个用户可有多个角色、即多个 data scope。
_Avoid_: 用「数据权限」指代它(数据权限是整个机制,data scope 是其中一档范围)。

**Declaration Adapter (声明适配器)**:
四层里的「声明」一层:`@DataPermission` 注解(+ `@DataColumn` 字段→列映射)作为 mapper 上的声明入口,加 `DataPermissionAdvice` 切面在方法调用时把注解捕获进 `PERMISSION_CACHE`。深化中保持不变;PERMISSION_CACHE 的「每次调用 set+clear」生命周期归它主管。
_Avoid_: 「注解处理器」(易和 SpEL/MyBatis 处理器混)。

**MyBatis Adapter (SQL 执行适配器)**:
四层里的「执行」一层:变薄后的 `PlusDataPermissionInterceptor`,只负责识别 SELECT/UPDATE/DELETE、向 policy 要 DataFilter、把 `Predicate` 的 SQL 文本 re-parse 后 AND 进 where。**只碰 SQL-AST,不算规则**。
_Avoid_: handler(现耦合实现);「数据权限拦截器」笼统指代(它深化后只剩挂载职责)。

**Policy (数据权限策略 / DataPermissionPolicy)**:
深化后的**纯函数式核心**:输入(声明 + 授权快照 + 操作类型),输出一个 Data Filter,全程不接触 servlet / Sa-Token / MyBatis 执行。承接现 `PlusDataPermissionHandler` 中的规则计算。
_Avoid_: handler(`PlusDataPermissionHandler` 是正被取代的耦合实现,不要用它指代纯策略)。

**Data Filter (DataFilter)**:
Policy 的**输出**,一个三态值:`UNRESTRICTED`(不追加任何条件)、`DENY`(拒绝一切,语义等价旧的 `1 = 0`)、`Predicate`(携带一段 SQL 谓词文本)。三态显式化是深化的目标之一——尤其让 DENY 成为可断言的独立语义,而非「恰好恒假的普通谓词串」。
_Avoid_: 「过滤条件字符串 / filterSql」(那只是 Predicate 这一态的载荷,不是 DataFilter 本身)。

**Predicate (谓词)**:
Data Filter 的一态,载荷是一段 **SQL 文本**(如 `dept_id = 100`)。由 MyBatis adapter 负责 re-parse 成表达式、括号化后 AND 进原 where。seam 是 stringly-typed 是有意为之(见 issue #1 路线 A):文本来自 SpEL 模板,re-parse 这一步无法消除、只能归位到 SQL-AST 一侧。
_Avoid_: condition、where 片段(歧义)。

**Authorization Context (AuthorizationContext / 授权快照)**:
Policy 的授权输入——一个**不可变快照**,定格规则计算所需的全部事实,六个字段:`superAdmin`、`user`、`roles`、`dataScopeRoleMap`、`access`、`variables`。由 context provider 适配器在「执行查询的那个线程上」组装。`user/access/roles/dataScopeRoleMap` 一律是类型化字段;`variables` 只装「用户额外设置的」SpEL 变量,**不得**再混入 user/access(否则会被往 SpEL 塞两遍)。
_Avoid_: 把它和 `Variables`(SpEL 变量袋子)、SpEL 的 `EvaluationContext`、Sa-Token storage 混称「context」。

**Variables (SpEL 变量袋子)**:
经 `DataPermissionHelper.setVariable` 设入、供 SpEL 模板引用的额外键值对。现由 `DataPermissionHelper.getContext()` 暴露——该方法**命名错误**(它不是「上下文」),计划改名 `getVariables()`。只承载「额外变量」,框架事实(user/access)走 Authorization Context 的类型化字段。
_Avoid_: getContext()、「数据权限上下文」(那是 Authorization Context)。

**Context Provider (上下文适配器)**:
组装 `AuthorizationContext` 的窄适配器,收拢现散在 handler 里的 `currentUser` / `currentAccess` / `resolveAccess`,从 Sa-Token storage、servlet request、controller 注解读取并定格快照。**懒拉**:由拦截器在「正在执行该查询的那个线程」上调用——普通请求即请求线程;查询若派到虚拟线程则在子 VT 上拉、storage 未初始化即抛(故跨 VT 红灯探针**保持红**:本次只搬家上下文获取,不修跨线程传播,后者另立工单)。沿用请求级缓存(读不到才解析、回写 Sa-Token storage)。
_Avoid_: 把它和 policy 混——provider 负责「取事实」,policy 负责「算规则」;provider 碰框架,policy 纯。

**Operation Kind (OperationKind / 操作类型)**:
Policy 的**独立入参**(不进快照),枚举 SELECT / UPDATE / DELETE,各自携带默认连接词:SELECT 用 ` OR `(更宽松,取并集),UPDATE/DELETE 用 ` AND `(更保守,取交集);可被注解 `joinStr` 覆盖。
_Avoid_: isSelect 布尔(旧写法;升级成枚举正是为了让「连接语义」成为显式输入而非隐藏 flag)。

**Data Scope Service (sdss / IDataScopeService)**:
「范围展开器」——给定 roleId 或 deptId,查库返回一串逗号分隔的部门 id(如 `"100,101,102"`,无则 `"-1"`),供模板拼进 `dept_id IN ( ... )`。**凡 SpEL 模板文本引用 `@sdss` 的 scope 都经它展开**——当前是 CUSTOM(`getRoleCustom` 查 `sys_role_dept`)、DEPT_AND_CHILD 与 DEPT_AND_CHILD_OR_SELF(后两者共用 `getDeptAndChild` 展开部门树);其余 scope(DEPT/SELF)静态写出、不碰它。注意 sdss **只有 2 个方法却被 3 个 scope 引用**(`getDeptAndChild` 被第 4、6 档共用)——「方法数」与「调用它的 scope 数」是两码事,别再把 2 当成 scope 数(第 6 档是**动态部门展开 `OR` 静态本人**的混合范围,二分「动态/静态」容纳不下它)。bean 名 `sdss`。深化后其 2 方法接口提升到 `ruoyi-api`(`IDataScopeService`),作为**类型化 SPI 注入 policy**,使 `common-mybatis` 无需依赖 `ruoyi-system`、且 policy 不再静态触达 Spring 容器。
_Avoid_: 「数据权限服务」(太泛);把「sdss 的方法数(2)」当成「调用 sdss 的 scope 数(3)」。

**Recursion Guard (RecursionGuard)**:
注入给 policy 的「防递归开关」:`sdss` 自身要跑 `sys_role_dept`/`sys_dept` 查询,而这些查询又会触发数据权限拦截器 → 再调 sdss → 死循环;故展开范围前临时关掉数据权限。生产委托 `DataPermissionHelper.ignore`,测试给直接执行的 no-op。
_Avoid_: ignore(动词,易和 MP 的 `@InterceptorIgnore` 混)。

**Access (访问约束 / DataPermissionAccess)**:
从当前请求的 controller 端点解析出的权限约束:`@SaCheckPermission` 的 perms + orRole、`@SaCheckRole` 的 roleKeys。`constrained()` 为真时,只有匹配该约束的角色参与数据权限计算;约束存在却无匹配角色 → Data Filter 为 `DENY`。解析(读 servlet/HandlerMethod/注解)属 context provider;**用 access 筛角色**(scopeRoles)属 policy。
_Avoid_: 把「解析 access」和「用 access 筛角色」混为一谈——前者是适配器,后者是策略,这条线是干净 seam 的所在。
