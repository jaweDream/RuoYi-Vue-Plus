package org.dromara.chain.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 链资产配置表 chain_asset
 *
 * <p>登记需要扫块入账/支持转账的资产;扫块任务只关注状态正常的资产。
 *
 * @author jarvey
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chain_asset")
public class ChainAsset extends BaseEntity {

    /**
     * 资产ID
     */
    @TableId(value = "asset_id")
    private Long assetId;

    /**
     * 链协议(EVM/SOLANA/APTOS)
     */
    private String protocol;

    /**
     * 网络名(chain.networks 配置键)
     */
    private String network;

    /**
     * 资产类型(NATIVE原生币 TOKEN合约代币)
     */
    private String assetType;

    /**
     * 资产符号(如 ETH/USDT)
     */
    private String symbol;

    /**
     * 资产名称
     */
    private String assetName;

    /**
     * 合约标识(EVM合约地址/Solana mint/Aptos coin type;原生币为空)
     */
    private String contractAddress;

    /**
     * 精度(最小单位小数位数)
     */
    private Integer decimals;

    /**
     * 状态(0正常 1停用)
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

}
