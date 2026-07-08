package org.dromara.chain.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.dromara.chain.domain.ChainTransferTask;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 链转账任务业务对象 chain_transfer_task(建单入参 + 列表查询条件)
 *
 * @author jarvey
 */
@Data
@AutoMapper(target = ChainTransferTask.class, reverseConvertGenerate = false)
public class ChainTransferTaskBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 业务单号(上游幂等键)
     */
    @NotBlank(message = "业务单号不能为空")
    @Size(max = 64, message = "业务单号不能超过{max}个字符")
    private String bizNo;

    /**
     * 网络名
     */
    @NotBlank(message = "网络名不能为空")
    private String network;

    /**
     * 资产ID
     */
    @NotNull(message = "资产不能为空")
    private Long assetId;

    /**
     * 出账地址(热钱包)
     */
    @NotBlank(message = "出账地址不能为空")
    @Size(max = 256, message = "出账地址不能超过{max}个字符")
    private String fromAddress;

    /**
     * 入账地址
     */
    @NotBlank(message = "入账地址不能为空")
    @Size(max = 256, message = "入账地址不能超过{max}个字符")
    private String toAddress;

    /**
     * 金额(最小单位原始值,整数)
     */
    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "1", message = "金额必须为正整数(最小单位)")
    private BigDecimal rawAmount;

    /**
     * 备注/附言
     */
    @Size(max = 256, message = "附言不能超过{max}个字符")
    private String memo;

    /**
     * 状态(查询条件用)
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

}
