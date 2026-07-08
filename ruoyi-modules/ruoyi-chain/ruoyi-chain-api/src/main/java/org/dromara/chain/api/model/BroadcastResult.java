package org.dromara.chain.api.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 交易广播结果
 *
 * @author jarvey
 */
@Data
@Builder
public class BroadcastResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 节点是否接受该交易
     */
    private boolean accepted;

    /**
     * 交易哈希(接受时必有)
     */
    private String txHash;

    /**
     * 节点返回信息(拒绝原因等)
     */
    private String message;

}
