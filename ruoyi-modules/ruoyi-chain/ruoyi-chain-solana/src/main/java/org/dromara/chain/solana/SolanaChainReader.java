package org.dromara.chain.solana;

import org.dromara.chain.api.enums.FinalityType;
import org.dromara.chain.api.enums.TxStatus;
import org.dromara.chain.api.exception.ChainAdapterException;
import org.dromara.chain.api.model.AssetTransferEvent;
import org.dromara.chain.api.model.ChainFinality;
import org.dromara.chain.api.model.ChainKey;
import org.dromara.chain.api.model.ChainNetworkConfig;
import org.dromara.chain.api.model.WatchedAsset;
import org.dromara.chain.api.spi.ChainReader;

import java.util.Collection;
import java.util.List;

/**
 * Solana 链读实现(骨架,待实现)
 *
 * <p>实现要点(JSON-RPC,建议承诺级别 finalized):
 * <ul>
 *     <li>高度语义:统一用 slot(getSlot 取链头;slot 可能跳空,getBlock 对空 slot 返回错误码
 *     -32007/-32009,应视为"无区块"返回空列表而非异常)</li>
 *     <li>转账解析:getBlock(slot, transactionDetails=full, jsonParsed) 后,原生 SOL 看
 *     system 指令 transfer,SPL Token 看 spl-token 指令 transfer/transferChecked,
 *     mint 匹配受关注资产;入账地址需从 token account 反解出 owner</li>
 *     <li>交易状态:getSignatureStatuses / getTransaction,finalized 即终局</li>
 * </ul>
 *
 * @author jarvey
 */
public class SolanaChainReader implements ChainReader {

    private final ChainNetworkConfig config;

    public SolanaChainReader(ChainNetworkConfig config) {
        this.config = config;
    }

    @Override
    public ChainKey chainKey() {
        return config.getChainKey();
    }

    @Override
    public ChainFinality finality() {
        // finalized 承诺级别下不可回滚,无需额外确认深度
        return ChainFinality.of(FinalityType.DETERMINISTIC, 0);
    }

    @Override
    public long latestHeight() {
        throw ChainAdapterException.unsupported("Solana getSlot(finalized) 查询链头");
    }

    @Override
    public List<AssetTransferEvent> extractTransfers(long height, Collection<WatchedAsset> assets) {
        throw ChainAdapterException.unsupported("Solana getBlock 区块转账解析(SOL/SPL Token)");
    }

    @Override
    public TxStatus getTxStatus(String txHash) {
        throw ChainAdapterException.unsupported("Solana getSignatureStatuses 交易状态查询");
    }

}
