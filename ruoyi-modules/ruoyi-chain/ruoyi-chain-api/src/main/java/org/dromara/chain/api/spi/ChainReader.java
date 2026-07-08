package org.dromara.chain.api.spi;

import org.dromara.chain.api.enums.TxStatus;
import org.dromara.chain.api.model.AssetTransferEvent;
import org.dromara.chain.api.model.ChainFinality;
import org.dromara.chain.api.model.ChainKey;
import org.dromara.chain.api.model.WatchedAsset;

import java.util.Collection;
import java.util.List;

/**
 * 链读能力 SPI(面向单个网络的只读原语)
 *
 * <p>适配器只提供"某高度上发生了哪些受关注资产的转账"这一原语,
 * 游标推进、确认深度、地址匹配、入账幂等等编排逻辑全部由业务层负责,
 * 适配器实现必须无状态(除持有 RPC 连接配置外)。
 *
 * @author jarvey
 */
public interface ChainReader {

    /**
     * 本实例服务的链网络
     *
     * @return 链网络标识
     */
    ChainKey chainKey();

    /**
     * 链终局性描述(业务层据此与配置共同决定安全扫描高度)
     *
     * @return 终局性描述
     */
    ChainFinality finality();

    /**
     * 查询链头高度(Solana 为 slot,Aptos 为 ledger version)
     *
     * @return 链头高度
     */
    long latestHeight();

    /**
     * 解析指定高度上、命中受关注资产的全部转账事件
     *
     * <p>返回该高度的全量命中事件,不做收付款地址过滤(归属判断是业务层的职责);
     * 高度上无区块(如 Solana 跳空 slot)或无命中事件时返回空列表。
     *
     * @param height 区块高度
     * @param assets 受关注资产集合(同一网络)
     * @return 归一化转账事件列表
     */
    List<AssetTransferEvent> extractTransfers(long height, Collection<WatchedAsset> assets);

    /**
     * 查询交易状态
     *
     * @param txHash 交易哈希
     * @return 归一化交易状态
     */
    TxStatus getTxStatus(String txHash);

}
