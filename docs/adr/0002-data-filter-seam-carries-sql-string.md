# DataFilter seam 携带 SQL 字符串,而非结构化表达式

policy 与 MyBatis 适配器之间的 seam 传递 `DataFilter`,三态:`UNRESTRICTED` / `DENY` / `Predicate(String sql)`。`Predicate` 的载荷是一段 SQL 文本(如 `dept_id = 100`),由适配器 re-parse 成 jsqlparser 表达式、括号化后 AND 进原 where。

选字符串而非直接产出结构化 `Expression`,因为 scope 模板本就是 SpEL → 字符串,这次「文本→AST」的 re-parse **无法消除、只能归位**到 SQL-AST 一侧(适配器);本次深化的价值在于「policy 成为纯函数」,而非「载荷结构化」。

将谓词升级为结构化生成是明确的**后续工单**,不在本次范围。future reader 若疑惑「为何 policy 返回字符串让适配器再解析一遍」,答案在此。
