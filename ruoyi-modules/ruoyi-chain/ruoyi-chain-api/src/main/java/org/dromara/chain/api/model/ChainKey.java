package org.dromara.chain.api.model;

import lombok.Value;
import org.dromara.chain.api.enums.ProtocolType;

import java.io.Serial;
import java.io.Serializable;

/**
 * 链网络唯一标识(协议 + 网络名)
 *
 * <p>网络名为业务自定义的稳定键(如 {@code eth-mainnet}、{@code bsc}、{@code sol-mainnet}),
 * 与配置 {@code chain.networks.<network>} 的 key 一致,也是业务表中 network 列的取值。
 *
 * @author jarvey
 */
@Value(staticConstructor = "of")
public class ChainKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 链协议类型
     */
    ProtocolType protocol;

    /**
     * 网络名(配置键,全局唯一)
     */
    String network;

    /**
     * 规范化字符串形式,形如 {@code evm:eth-mainnet},用于日志与缓存键。
     *
     * @return 规范化标识
     */
    public String canonical() {
        return protocol.name().toLowerCase() + ":" + network;
    }

}
