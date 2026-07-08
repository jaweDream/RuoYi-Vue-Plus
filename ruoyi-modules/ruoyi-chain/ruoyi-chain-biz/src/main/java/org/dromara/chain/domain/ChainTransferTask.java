package org.dromara.chain.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;

/**
 * 链转账任务表 chain_transfer_task
 *
 * <p>业务幂等键:biz_no 唯一索引(上游业务单号,重复建单直接拒绝);
 * 状态机见 {@link org.dromara.chain.enums.ChainTransferStatus}。
 *
 * @author jarvey
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chain_transfer_task")
public class ChainTransferTask extends BaseEntity {

    /**
     * 任务ID
     */
    @TableId(value = "task_id")
    private Long taskId;

    /**
     * 业务单号(上游幂等键,唯一)
     */
    private String bizNo;

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
     * 出账地址(热钱包)
     */
    private String fromAddress;

    /**
     * 入账地址
     */
    private String toAddress;

    /**
     * 金额(最小单位原始值)
     */
    private BigDecimal rawAmount;

    /**
     * 备注/附言(链支持时写入)
     */
    private String memo;

    /**
     * 状态(0待执行 1执行中 2已广播 3成功 4失败)
     */
    private String status;

    /**
     * 交易哈希(广播成功后回填)
     */
    private String txHash;

    /**
     * 估算手续费(原生币最小单位)
     */
    private BigDecimal rawFee;

    /**
     * 已执行次数
     */
    private Integer attemptCount;

    /**
     * 最近一次失败原因
     */
    private String lastError;

    /**
     * 备注
     */
    private String remark;

}
