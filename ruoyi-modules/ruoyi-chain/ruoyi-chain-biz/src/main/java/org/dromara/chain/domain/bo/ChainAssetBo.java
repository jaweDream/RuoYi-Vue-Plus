package org.dromara.chain.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.dromara.chain.domain.ChainAsset;

import java.io.Serial;
import java.io.Serializable;

/**
 * 链资产配置业务对象 chain_asset
 *
 * @author jarvey
 */
@Data
@AutoMapper(target = ChainAsset.class, reverseConvertGenerate = false)
public class ChainAssetBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 资产ID
     */
    private Long assetId;

    /**
     * 链协议(EVM/SOLANA/APTOS)
     */
    @NotBlank(message = "链协议不能为空")
    private String protocol;

    /**
     * 网络名
     */
    @NotBlank(message = "网络名不能为空")
    @Size(max = 64, message = "网络名不能超过{max}个字符")
    private String network;

    /**
     * 资产类型(NATIVE原生币 TOKEN合约代币)
     */
    @NotBlank(message = "资产类型不能为空")
    private String assetType;

    /**
     * 资产符号
     */
    @NotBlank(message = "资产符号不能为空")
    @Size(max = 32, message = "资产符号不能超过{max}个字符")
    private String symbol;

    /**
     * 资产名称
     */
    @Size(max = 128, message = "资产名称不能超过{max}个字符")
    private String assetName;

    /**
     * 合约标识(TOKEN 必填)
     */
    @Size(max = 256, message = "合约标识不能超过{max}个字符")
    private String contractAddress;

    /**
     * 精度(最小单位小数位数)
     */
    @NotNull(message = "精度不能为空")
    @Min(value = 0, message = "精度不能小于{value}")
    @Max(value = 38, message = "精度不能大于{value}")
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
