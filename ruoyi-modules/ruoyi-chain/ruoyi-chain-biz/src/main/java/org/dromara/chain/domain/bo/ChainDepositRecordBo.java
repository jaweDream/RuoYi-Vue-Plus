package org.dromara.chain.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 链充值记录查询对象(记录由扫块任务写入,不提供人工增改)
 *
 * @author jarvey
 */
@Data
public class ChainDepositRecordBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 网络名
     */
    private String network;

    /**
     * 资产ID
     */
    private Long assetId;

    /**
     * 交易哈希
     */
    private String txHash;

    /**
     * 转入地址
     */
    private String toAddress;

    /**
     * 状态(0已确认 1已上账)
     */
    private String status;

}
