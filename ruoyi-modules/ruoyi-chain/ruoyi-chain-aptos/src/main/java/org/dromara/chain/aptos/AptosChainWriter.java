package org.dromara.chain.aptos;

import org.dromara.chain.api.exception.ChainAdapterException;
import org.dromara.chain.api.model.BroadcastResult;
import org.dromara.chain.api.model.ChainKey;
import org.dromara.chain.api.model.ChainNetworkConfig;
import org.dromara.chain.api.model.FeeEstimate;
import org.dromara.chain.api.model.PreparedTx;
import org.dromara.chain.api.model.SignedTx;
import org.dromara.chain.api.model.TransferRequest;
import org.dromara.chain.api.spi.ChainWriter;

/**
 * Aptos 链写实现(骨架,待实现)
 *
 * <p>实现要点:组交易需取账户 sequence_number 与 gas 参数(estimate_gas_price),
 * payload 为 entry function(0x1::aptos_account::transfer 或 coin/FA 对应函数),
 * 交易带 expiration_timestamp_secs 过期时间;广播 POST /transactions(BCS 或 JSON+签名)。
 *
 * @author jarvey
 */
public class AptosChainWriter implements ChainWriter {

    private final ChainNetworkConfig config;

    public AptosChainWriter(ChainNetworkConfig config) {
        this.config = config;
    }

    @Override
    public ChainKey chainKey() {
        return config.getChainKey();
    }

    @Override
    public FeeEstimate estimateFee(TransferRequest request) {
        throw ChainAdapterException.unsupported("Aptos estimate_gas_price 估费");
    }

    @Override
    public PreparedTx buildTransfer(TransferRequest request) {
        throw ChainAdapterException.unsupported("Aptos 转账组交易(sequence_number/过期时间)");
    }

    @Override
    public BroadcastResult broadcast(SignedTx tx) {
        throw ChainAdapterException.unsupported("Aptos POST /transactions 广播");
    }

}
