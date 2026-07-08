package org.dromara.chain.api.model;

import lombok.Builder;
import lombok.Data;
import org.dromara.chain.api.enums.AssetType;

import java.io.Serial;
import java.io.Serializable;

/**
 * 受关注资产(扫块与转账的资产口径,由业务层从资产配置表构造)
 *
 * @author jarvey
 */
@Data
@Builder
public class WatchedAsset implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务资产 ID(chain_asset 主键),适配器原样带回事件中,不解释含义
     */
    private Long assetId;

    /**
     * 所属链网络
     */
    private ChainKey chainKey;

    /**
     * 资产类型
     */
    private AssetType assetType;

    /**
     * 资产符号(如 ETH/USDT),仅用于日志展示
     */
    private String symbol;

    /**
     * 合约标识:EVM 合约地址 / Solana mint 地址 / Aptos coin type 或 FA metadata 地址;
     * 原生币为空
     */
    private String contractAddress;

    /**
     * 精度(最小单位小数位数,如 ETH=18、USDT(ERC20)=6)
     */
    private int decimals;

}
