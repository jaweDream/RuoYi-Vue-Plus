package org.dromara.chain.api.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 已组装待签名交易
 *
 * <p>payload 与 meta 的具体格式由协议适配器定义并由同协议的 {@code ChainSigner} 消费,
 * 业务层只负责在两者之间透传,不解释内容。
 *
 * @author jarvey
 */
@Data
@Builder
public class PreparedTx implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属链网络
     */
    private ChainKey chainKey;

    /**
     * 待签名载荷(协议自定义编码,如 EVM 为交易字段 JSON)
     */
    private String payload;

    /**
     * 组交易上下文(如 nonce/gasPrice/chainId、blockhash、sequence_number 等)
     */
    @Builder.Default
    private Map<String, String> meta = new HashMap<>();

}
