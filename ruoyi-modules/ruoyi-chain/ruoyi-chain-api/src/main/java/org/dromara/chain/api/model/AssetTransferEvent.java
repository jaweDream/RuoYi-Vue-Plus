package org.dromara.chain.api.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.time.Instant;

/**
 * 归一化资产转账事件(适配器解析区块后的统一输出)
 *
 * <p>幂等键为 (network, txHash, eventIndex):同一交易内可能包含多条代币转账事件,
 * 以事件序号区分;原生币转账约定 {@code eventIndex = -1}(一笔交易至多一次原生价值转移)。
 *
 * @author jarvey
 */
@Data
@Builder
public class AssetTransferEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 原生币转账的事件序号约定值
     */
    public static final int NATIVE_EVENT_INDEX = -1;

    /**
     * 所属链网络
     */
    private ChainKey chainKey;

    /**
     * 命中的业务资产 ID(与传入的 WatchedAsset 对应)
     */
    private Long assetId;

    /**
     * 交易哈希
     */
    private String txHash;

    /**
     * 交易内事件序号(EVM 为 logIndex;原生币转账固定为 {@link #NATIVE_EVENT_INDEX})
     */
    private int eventIndex;

    /**
     * 区块高度(Solana 为 slot,Aptos 为 version)
     */
    private long height;

    /**
     * 区块哈希(留作重组核对依据)
     */
    private String blockHash;

    /**
     * 区块时间
     */
    private Instant blockTime;

    /**
     * 转出地址
     */
    private String fromAddress;

    /**
     * 转入地址
     */
    private String toAddress;

    /**
     * 转账金额(最小单位原始值,不做精度换算)
     */
    private BigInteger rawAmount;

}
