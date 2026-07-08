# ruoyi-chain 链资产模块

## 设计:两个正交维度,两条轴分开放

- **链轴(协议差异)** → 适配器模块 `ruoyi-chain-evm/solana/aptos`,只回答"这条链怎么读块、怎么组交易、怎么广播",实现 `ruoyi-chain-api` 的 SPI,**不含任何业务编排**。
- **功能轴(扫块/转账)** → `ruoyi-chain-biz` 里的两个 Service 族,链无关地编排"什么时候读、什么时候发":游标推进、确认深度、地址归属匹配、入账幂等、转账状态机。

新增协议 = 新增一个适配器模块并注册 `ChainClientFactory` Bean,biz 零改动。

## 模块结构

```
ruoyi-chain/
├── ruoyi-chain-api      # SPI(ChainReader/ChainWriter/ChainSigner/ChainClientFactory)
│                        # + 归一化模型(ChainKey/WatchedAsset/AssetTransferEvent/PreparedTx...)
│                        # 零 Spring/MyBatis 依赖
├── ruoyi-chain-evm      # EVM 适配器:JSON-RPC 直连(JDK HttpClient),读路径+估费+广播真实实现
├── ruoyi-chain-solana   # 骨架:方法抛"未实现",类 JavaDoc 记录实现要点(slot/finalized/ATA)
├── ruoyi-chain-aptos    # 骨架:同上(version/REST fullnode/sequence_number)
└── ruoyi-chain-biz      # 业务层:5 张表 + 扫块编排 + 转账状态机 + SnailJob 任务 + 管理接口
```

依赖方向:`biz → api ← 适配器`;biz 不依赖任何适配器,`ruoyi-admin` 同时引入 biz 与需要的适配器(可插拔)。

## 关键概念

- **ChainKey = protocol + network**:协议(EVM)是适配器单位,网络(eth-mainnet/bsc)是实例单位;
  network 字符串同时是 `chain.networks` 配置键与所有业务表的 `network` 列取值。
- **金额一律最小单位原始值**(wei/lamports),DB 用 `decimal(65,0)`,展示层按资产 `decimals` 换算。
- **高度归一化**:EVM=块高,Solana=slot,Aptos=version;游标语义统一为"已扫描高度,下次从 +1 开始"。

## 安全不变量(改代码前先读)

1. **任一高度解析失败不推进游标**——推进即漏账;游标停在原地记录 lastError,等下轮任务重试。
2. **入账幂等靠 `(network, tx_hash, event_index)` 唯一键**(原生币转账 event_index=-1),
   重扫/回扫/并发都安全,所以游标重置(回扫)是无害运维操作。
3. **只扫"安全高度"**:链头 − 确认深度(EVM 默认 12,可按网络配置覆盖;Solana/Aptos 终局链为 0)。
   深度重组超过确认数的场景未自动处理,靠 block_hash 留痕人工核对。
4. **转账状态机用条件更新抢占**(待执行→执行中),防并发重复出账;执行路径不包数据库事务
   (链上广播不可回滚)。广播通信失败存在"实际已上链"的不确定性,重试前须人工核对 nonce/txHash。
5. **签名不落库不内置**:`ChainSigner` SPI 按协议注册,本仓库无任何持钥实现;未注册时转账在
   签名环节明确失败。生产接入 KMS/MPC/web3j 本地签名自行实现。

## 配置样例(ruoyi-admin application-*.yml)

```yaml
chain:
  enabled: true            # 总开关,默认 false(不配置也能启动,仅管理界面可用)
  networks:
    eth-mainnet:
      protocol: EVM
      rpc-url: https://ethereum.publicnode.com
      confirmations: 12
      scan-batch-size: 20
      # start-height: 23000000   # 首次建游标起点,缺省=当前链头(不回扫历史)
```

SnailJob 调度中心需注册两个任务:`chainScanJob`(建议 15~30s 固定间隔)、`chainTransferStatusJob`(建议 1min)。
表结构与菜单:`script/sql/ry_chain.sql`。

## 当前状态 / 待办

- [x] EVM 读路径(原生+ERC20 Transfer 解析、交易状态)、估费、广播;签名走 SPI 外置
- [ ] EVM 签名器参考实现(web3j 本地签名,建议放独立 starter 或 demo)
- [ ] Solana / Aptos 适配器实现(骨架类 JavaDoc 已写明要点)
- [ ] 内部转账(EVM trace)、深度重组自动回滚、按地址 nonce 台账与串行化
- [ ] 充值记录"已上账"状态的业务侧消费接口(当前仅落库为"已确认")
- [ ] 集成测试:用 Fake ChainReader/Writer Bean 测扫块幂等与转账状态机(参照 docs/测试规范.md)
