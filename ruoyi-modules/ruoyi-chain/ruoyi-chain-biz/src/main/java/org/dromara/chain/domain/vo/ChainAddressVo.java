package org.dromara.chain.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.chain.domain.ChainAddress;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 链托管地址视图对象 chain_address
 *
 * @author jarvey
 */
@Data
@AutoMapper(target = ChainAddress.class)
public class ChainAddressVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 地址ID
     */
    private Long addressId;

    /**
     * 链协议(EVM/SOLANA/APTOS)
     */
    private String protocol;

    /**
     * 网络名
     */
    private String network;

    /**
     * 链上地址
     */
    private String address;

    /**
     * 地址类型(0充值地址 1热钱包)
     */
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

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
