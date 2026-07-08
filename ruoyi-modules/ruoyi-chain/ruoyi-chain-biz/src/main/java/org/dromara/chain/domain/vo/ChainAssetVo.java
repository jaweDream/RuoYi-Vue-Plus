package org.dromara.chain.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.chain.domain.ChainAsset;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 链资产配置视图对象 chain_asset
 *
 * @author jarvey
 */
@Data
@AutoMapper(target = ChainAsset.class)
public class ChainAssetVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 资产ID
     */
    private Long assetId;

    /**
     * 链协议(EVM/SOLANA/APTOS)
     */
    private String protocol;

    /**
     * 网络名
     */
    private String network;

    /**
     * 资产类型(NATIVE原生币 TOKEN合约代币)
     */
    private String assetType;

    /**
     * 资产符号
     */
    private String symbol;

    /**
     * 资产名称
     */
    private String assetName;

    /**
     * 合约标识
     */
    private String contractAddress;

    /**
     * 精度
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

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
