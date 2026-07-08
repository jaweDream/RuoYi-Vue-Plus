package org.dromara.chain.evm;

import org.dromara.chain.api.enums.ProtocolType;
import org.dromara.chain.api.model.ChainNetworkConfig;
import org.dromara.chain.api.spi.ChainClientFactory;
import org.dromara.chain.api.spi.ChainReader;
import org.dromara.chain.api.spi.ChainWriter;
import org.springframework.stereotype.Component;

/**
 * EVM 适配器工厂(同一实现服务所有 EVM 兼容网络,按网络配置实例化)
 *
 * @author jarvey
 */
@Component
public class EvmChainClientFactory implements ChainClientFactory {

    @Override
    public ProtocolType protocol() {
        return ProtocolType.EVM;
    }

    @Override
    public ChainReader createReader(ChainNetworkConfig config) {
        return new EvmChainReader(config, new EvmJsonRpcClient(config.getRpcUrl()));
    }

    @Override
    public ChainWriter createWriter(ChainNetworkConfig config) {
        return new EvmChainWriter(config, new EvmJsonRpcClient(config.getRpcUrl()));
    }

}
