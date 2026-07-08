package org.dromara.chain.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.dromara.chain.domain.ChainAddress;

import java.io.Serial;
import java.io.Serializable;

/**
 * 链托管地址业务对象 chain_address
 *
 * @author jarvey
 */
@Data
@AutoMapper(target = ChainAddress.class, reverseConvertGenerate = false)
public class ChainAddressBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 地址ID
     */
    private Long addressId;

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
     * 链上地址
     */
    @NotBlank(message = "链上地址不能为空")
    @Size(max = 256, message = "链上地址不能超过{max}个字符")
    private String address;

    /**
     * 地址类型(0充值地址 1热钱包)
     */
    @NotBlank(message = "地址类型不能为空")
    private String addressType;

    /**
     * 归属业务用户ID
     */
    private Long userId;

    /**
     * 状态(0正常 1停用)
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

}
