package org.dromara.chain.domain.bo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 扫块游标重置业务对象(运维操作:回扫或跳块)
 *
 * @author jarvey
 */
@Data
public class ChainCursorResetBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 网络名
     */
    @NotBlank(message = "网络名不能为空")
    private String network;

    /**
     * 重置后的已扫描高度(下次从 +1 开始;充值入库有唯一键,回扫不会重复入账)
     */
    @NotNull(message = "高度不能为空")
    @Min(value = 0, message = "高度不能小于{value}")
    private Long scannedHeight;

}
