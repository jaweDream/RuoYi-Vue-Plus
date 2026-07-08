package org.dromara.chain.api.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 链网络接入配置(由业务层解析配置后传给适配器工厂)
 *
 * @author jarvey
 */
@Data
@Builder
public class ChainNetworkConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 链网络标识
     */
    private ChainKey chainKey;

    /**
     * 节点 RPC 端点(EVM/Solana 为 JSON-RPC URL,Aptos 为 fullnode REST 根路径)
     */
    private String rpcUrl;

    /**
     * 入账确认深度覆盖值;为空时使用适配器按协议给出的默认值
     */
    private Integer confirmations;

    /**
     * 协议相关扩展参数(如鉴权 header、承诺级别等),由具体适配器解释
     */
    @Builder.Default
    private Map<String, String> options = new HashMap<>();

}
