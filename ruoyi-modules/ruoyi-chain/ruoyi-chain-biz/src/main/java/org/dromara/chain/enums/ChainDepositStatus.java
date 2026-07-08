package org.dromara.chain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 链充值记录状态
 *
 * @author jarvey
 */
@Getter
@AllArgsConstructor
public enum ChainDepositStatus {

    /**
     * 已确认(达到确认深度并入库,尚未同步给业务账务)
     */
    CONFIRMED("0", "已确认"),

    /**
     * 已上账(业务账务已消费该记录)
     */
    CREDITED("1", "已上账");

    /**
     * 存库状态码
     */
    private final String code;

    /**
     * 状态说明
     */
    private final String info;

}
