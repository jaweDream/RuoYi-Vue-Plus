package org.dromara.chain.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.chain.domain.ChainDepositRecord;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 链充值记录视图对象 chain_deposit_record
 *
 * @author jarvey
 */
@Data
@AutoMapper(target = ChainDepositRecord.class)
public class ChainDepositRecordVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
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
     * 资产ID
     */
    private Long assetId;

    /**
     * 命中的托管地址ID
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
     * 区块哈希
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
     * 转入地址
     */
    private String toAddress;

    /**
     * 金额(最小单位原始值)
     */
    private BigDecimal rawAmount;

    /**
     * 状态(0已确认 1已上账)
     */
    private String status;

    /**
     * 入库时间
     */
    private LocalDateTime createTime;

}
