package org.dromara.chain.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.chain.domain.ChainScanCursor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 扫块游标视图对象 chain_scan_cursor
 *
 * @author jarvey
 */
@Data
@AutoMapper(target = ChainScanCursor.class)
public class ChainScanCursorVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 游标ID
     */
    private Long cursorId;

    /**
     * 链协议(EVM/SOLANA/APTOS)
     */
    private String protocol;

    /**
     * 网络名
     */
    private String network;

    /**
     * 已扫描高度
     */
    private Long scannedHeight;

    /**
     * 状态(0运行 1暂停)
     */
    private String status;

    /**
     * 最近一次失败原因
     */
    private String lastError;

    /**
     * 更新时间(最近一次推进/失败时间)
     */
    private LocalDateTime updateTime;

}
