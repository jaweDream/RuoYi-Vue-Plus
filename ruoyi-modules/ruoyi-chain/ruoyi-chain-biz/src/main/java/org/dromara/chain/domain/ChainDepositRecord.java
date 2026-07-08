package org.dromara.chain.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 链充值记录表 chain_deposit_record
 *
 * <p>幂等键:(network, tx_hash, event_index) 唯一索引;扫块重扫/并发写入靠它兜底。
 * 记录在达到确认深度后才写入,写入即视为已确认。
 *
 * @author jarvey
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chain_deposit_record")
public class ChainDepositRecord extends BaseEntity {

    /**
     * 记录ID
     */
    @TableId(value = "deposit_id")
    private Long depositId;

    /**
     * 链协议(EVM/SOLANA/APTOS)
     */
    private String protocol;

    /**
     * 网络名
     */
    private String network;

    /**
     * 资产ID(chain_asset)
     */
    private Long assetId;

    /**
     * 命中的托管地址ID(chain_address)
     */
    private Long addressId;

    /**
     * 交易哈希
     */
    private String txHash;

    /**
     * 交易内事件序号(原生币转账为 -1)
     */
    private Integer eventIndex;

    /**
     * 区块高度
     */
    private Long height;

    /**
     * 区块哈希(重组核对依据)
     */
    private String blockHash;

    /**
     * 区块时间
     */
    private LocalDateTime blockTime;

    /**
     * 转出地址
     */
    private String fromAddress;

    /**
     * 转入地址(托管地址)
     */
    private String toAddress;

    /**
     * 金额(最小单位原始值,精度换算由展示层按资产 decimals 处理)
     */
    private BigDecimal rawAmount;

    /**
     * 状态(0已确认 1已上账)
     */
    private String status;

}
