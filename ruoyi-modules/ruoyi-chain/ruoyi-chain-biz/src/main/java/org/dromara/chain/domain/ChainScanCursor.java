package org.dromara.chain.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 扫块游标表 chain_scan_cursor(每个网络一行)
 *
 * <p>游标只增不跳:某高度解析失败则停在原地等待下轮重试,靠充值记录唯一键保证重扫幂等。
 *
 * @author jarvey
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chain_scan_cursor")
public class ChainScanCursor extends BaseEntity {

    /**
     * 游标ID
     */
    @TableId(value = "cursor_id")
    private Long cursorId;

    /**
     * 链协议(EVM/SOLANA/APTOS)
     */
    private String protocol;

    /**
     * 网络名(唯一)
     */
    private String network;

    /**
     * 已扫描高度(下次从 +1 开始)
     */
    private Long scannedHeight;

    /**
     * 状态(0运行 1暂停)
     */
    private String status;

    /**
     * 最近一次失败原因(成功后清空)
     */
    private String lastError;

}
