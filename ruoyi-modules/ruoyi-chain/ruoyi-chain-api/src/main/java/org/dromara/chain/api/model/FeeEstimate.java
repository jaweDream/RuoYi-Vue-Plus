package org.dromara.chain.api.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * 手续费估算结果
 *
 * @author jarvey
 */
@Data
@Builder
public class FeeEstimate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 估算手续费(原生币最小单位,如 EVM 的 wei)
     */
    private BigInteger rawFee;

    /**
     * 手续费币种符号(即网络原生币符号)
     */
    private String feeSymbol;

    /**
     * 估算明细(如 gasPrice/gasLimit),仅用于展示与排障
     */
    @Builder.Default
    private Map<String, String> details = new HashMap<>();

}
