package org.dromara.chain.aptos;

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
 * Aptos 链读实现(骨架,待实现)
 *
 * <p>实现要点(fullnode REST API,/v1):
 * <ul>
 *     <li>高度语义:统一用 ledger version(GET / 取链头 ledger_version;version 与
 *     交易一一对应,扫描粒度即"每 version 一笔交易")</li>
 *     <li>转账解析:GET /transactions/by_version/{version},原生 APT 与 Coin 看
 *     0x1::coin 事件(WithdrawEvent/DepositEvent)或 payload,FA 资产看
 *     0x1::fungible_asset 事件,按 coin type / metadata 地址匹配受关注资产</li>
 *     <li>交易状态:GET /transactions/by_hash/{hash} 的 success 字段;上链即终局</li>
 * </ul>
 *
 * @author jarvey
 */
public class AptosChainReader implements ChainReader {

    private final ChainNetworkConfig config;

    public AptosChainReader(ChainNetworkConfig config) {
        this.config = config;
    }

    @Override
    public ChainKey chainKey() {
        return config.getChainKey();
    }

    @Override
    public ChainFinality finality() {
        // Aptos 上链即终局
        return ChainFinality.of(FinalityType.DETERMINISTIC, 0);
    }

    @Override
    public long latestHeight() {
        throw ChainAdapterException.unsupported("Aptos ledger version 查询链头");
    }

    @Override
    public List<AssetTransferEvent> extractTransfers(long height, Collection<WatchedAsset> assets) {
        throw ChainAdapterException.unsupported("Aptos 按 version 解析 Coin/FA 转账");
    }

    @Override
    public TxStatus getTxStatus(String txHash) {
        throw ChainAdapterException.unsupported("Aptos by_hash 交易状态查询");
    }

}
