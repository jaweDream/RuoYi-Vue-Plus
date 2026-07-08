package org.dromara.chain.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.chain.domain.ChainTransferTask;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 链转账任务视图对象 chain_transfer_task
 *
 * @author jarvey
 */
@Data
@AutoMapper(target = ChainTransferTask.class)
public class ChainTransferTaskVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 业务单号
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
     * 资产ID
     */
    private Long assetId;

    /**
     * 出账地址
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
     * 备注/附言
     */
    private String memo;

    /**
     * 状态(0待执行 1执行中 2已广播 3成功 4失败)
     */
    private String status;

    /**
     * 交易哈希
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

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
