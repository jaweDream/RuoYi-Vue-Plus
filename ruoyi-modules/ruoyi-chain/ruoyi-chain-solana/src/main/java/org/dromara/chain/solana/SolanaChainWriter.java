package org.dromara.chain.solana;

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
 * Solana 链写实现(骨架,待实现)
 *
 * <p>实现要点:组交易需取 recentBlockhash(getLatestBlockhash,约 150 slot 内有效,
 * 过期须重组重签,业务层的失败重试路径天然覆盖);SPL Token 转账需处理收款方
 * ATA(关联代币账户)不存在时的创建指令;广播用 sendTransaction,估费用 getFeeForMessage。
 *
 * @author jarvey
 */
public class SolanaChainWriter implements ChainWriter {

    private final ChainNetworkConfig config;

    public SolanaChainWriter(ChainNetworkConfig config) {
        this.config = config;
    }

    @Override
    public ChainKey chainKey() {
        return config.getChainKey();
    }

    @Override
    public FeeEstimate estimateFee(TransferRequest request) {
        throw ChainAdapterException.unsupported("Solana getFeeForMessage 估费");
    }

    @Override
    public PreparedTx buildTransfer(TransferRequest request) {
        throw ChainAdapterException.unsupported("Solana 转账组交易(blockhash/ATA 处理)");
    }

    @Override
    public BroadcastResult broadcast(SignedTx tx) {
        throw ChainAdapterException.unsupported("Solana sendTransaction 广播");
    }

}
