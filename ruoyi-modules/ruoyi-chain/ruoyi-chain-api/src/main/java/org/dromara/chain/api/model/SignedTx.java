package org.dromara.chain.api.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 已签名交易(可直接广播)
 *
 * @author jarvey
 */
@Data
@Builder
public class SignedTx implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属链网络
     */
    private ChainKey chainKey;

    /**
     * 签名后的原始交易(EVM 为 0x 前缀 hex,Solana/Aptos 为协议约定编码)
     */
    private String rawSigned;

}
