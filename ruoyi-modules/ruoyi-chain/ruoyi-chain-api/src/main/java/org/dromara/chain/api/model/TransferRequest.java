package org.dromara.chain.api.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;

/**
 * 转账请求(业务层构造,交给 ChainWriter 估费/组交易)
 *
 * @author jarvey
 */
@Data
@Builder
public class TransferRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属链网络
     */
    private ChainKey chainKey;

    /**
     * 转账资产
     */
    private WatchedAsset asset;

    /**
     * 出账地址
     */
    private String fromAddress;

    /**
     * 入账地址
     */
    private String toAddress;

    /**
     * 转账金额(最小单位原始值)
     */
    private BigInteger rawAmount;

    /**
     * 备注/附言(链支持时写入,如 memo/tag;EVM 忽略)
     */
    private String memo;

}
