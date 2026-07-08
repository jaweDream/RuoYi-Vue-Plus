package org.dromara.chain.aptos;

import org.dromara.chain.api.enums.ProtocolType;
import org.dromara.chain.api.model.ChainNetworkConfig;
import org.dromara.chain.api.spi.ChainClientFactory;
import org.dromara.chain.api.spi.ChainReader;
import org.dromara.chain.api.spi.ChainWriter;
import org.springframework.stereotype.Component;

/**
 * Aptos 适配器工厂
 *
 * @author jarvey
 */
@Component
public class AptosChainClientFactory implements ChainClientFactory {

    @Override
    public ProtocolType protocol() {
        return ProtocolType.APTOS;
    }

    @Override
    public ChainReader createReader(ChainNetworkConfig config) {
        return new AptosChainReader(config);
    }

    @Override
    public ChainWriter createWriter(ChainNetworkConfig config) {
        return new AptosChainWriter(config);
    }

}
