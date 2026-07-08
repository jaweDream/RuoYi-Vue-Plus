package org.dromara.chain.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 链托管地址表 chain_address
 *
 * <p>充值地址用于扫块入账的归属匹配;热钱包地址用于出账。
 * 地址统一按链原始格式存储,匹配时不区分大小写。
 *
 * @author jarvey
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chain_address")
public class ChainAddress extends BaseEntity {

    /**
     * 地址ID
     */
    @TableId(value = "address_id")
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
     * 归属业务用户ID(充值地址可选绑定)
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
