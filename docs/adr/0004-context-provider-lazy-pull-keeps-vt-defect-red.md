# context provider 在执行线程上懒拉;跨虚拟线程传播故意不修

context provider 在「**正在执行该查询的那个线程**」上现拉授权上下文,而非在请求线程预先拍成快照再随任务带进子线程。

这有两层意图:

1. **行为保真**——本次只是把上下文获取从 `buildDataFilter` 搬进 provider 适配器,获取时机(拦截器处理该条 SQL 时)不变。
2. **守住红灯**——查询若被派到虚拟线程,子 VT 的 Sa-Token storage 未初始化 → 组装快照即抛,`DataPermissionVirtualThreadIntegrationTest` 的缺陷复现探针**保持红**。跨虚拟线程的上下文传播是一条**独立工单**,本次不修。

## Consequences

- 若日后有人把 provider 改成「请求线程预先快照、传进子线程」,会意外把红灯探针「修绿」——那等于绕过本决定、悄悄改了语义,应改为在独立工单里正式实现传播(TTL / 显式重放 / ScopedValue)。
- 另一处 behavior-preserving 边界——分页 count 查询与 `getSqlSegment` per-statement 清理 PERMISSION_CACHE 的相互作用——同样留作**独立工单**,本次以分页回归测试钉死当前行为,不深挖修复。
