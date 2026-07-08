package org.dromara.chain.solana;

import org.dromara.chain.api.enums.ProtocolType;
import org.dromara.chain.api.model.ChainNetworkConfig;
import org.dromara.chain.api.spi.ChainClientFactory;
import org.dromara.chain.api.spi.ChainReader;
import org.dromara.chain.api.spi.ChainWriter;
import org.springframework.stereotype.Component;

/**
 * Solana 适配器工厂
 *
 * @author jarvey
 */
@Component
public class SolanaChainClientFactory implements ChainClientFactory {

    @Override
    public ProtocolType protocol() {
        return ProtocolType.SOLANA;
    }

    @Override
    public ChainReader createReader(ChainNetworkConfig config) {
        return new SolanaChainReader(config);
    }

    @Override
    public ChainWriter createWriter(ChainNetworkConfig config) {
        return new SolanaChainWriter(config);
    }

}
