package org.dromara.chain.evm;

import com.fasterxml.jackson.databind.JsonNode;
import org.dromara.chain.api.enums.AssetType;
import org.dromara.chain.api.enums.FinalityType;
import org.dromara.chain.api.enums.TxStatus;
import org.dromara.chain.api.exception.ChainAdapterException;
import org.dromara.chain.api.model.AssetTransferEvent;
import org.dromara.chain.api.model.ChainFinality;
import org.dromara.chain.api.model.ChainKey;
import org.dromara.chain.api.model.ChainNetworkConfig;
import org.dromara.chain.api.model.WatchedAsset;
import org.dromara.chain.api.spi.ChainReader;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EVM 链读实现
 *
 * <p>原生币转账来自 eth_getBlockByNumber 的交易列表(value &gt; 0),
 * ERC-20 转账来自 eth_getLogs 的 Transfer 事件(仅匹配受关注合约,过滤 ERC-721 的 4-topic 形态)。
 * 内部转账(合约内 value 转移)不在本实现覆盖范围,如需支持需接 trace 类接口。
 *
 * @author jarvey
 */
public class EvmChainReader implements ChainReader {

    /**
     * ERC-20 Transfer(address,address,uint256) 事件签名 topic
     */
    private static final String TRANSFER_TOPIC = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";

    /**
     * EVM 默认入账确认深度
     */
    private static final int DEFAULT_CONFIRMATIONS = 12;

    private final ChainNetworkConfig config;
    private final EvmJsonRpcClient rpc;

    public EvmChainReader(ChainNetworkConfig config, EvmJsonRpcClient rpc) {
        this.config = config;
        this.rpc = rpc;
    }

    @Override
    public ChainKey chainKey() {
        return config.getChainKey();
    }

    @Override
    public ChainFinality finality() {
        int confirmations = config.getConfirmations() != null ? config.getConfirmations() : DEFAULT_CONFIRMATIONS;
        return ChainFinality.of(FinalityType.PROBABILISTIC, confirmations);
    }

    @Override
    public long latestHeight() {
        return EvmCodec.hexToLong(rpc.call("eth_blockNumber").asText());
    }

    @Override
    public List<AssetTransferEvent> extractTransfers(long height, Collection<WatchedAsset> assets) {
        WatchedAsset nativeAsset = null;
        Map<String, WatchedAsset> tokenByContract = new HashMap<>();
        for (WatchedAsset asset : assets) {
            if (asset.getAssetType() == AssetType.NATIVE) {
                nativeAsset = asset;
            } else if (asset.getContractAddress() != null) {
                tokenByContract.put(asset.getContractAddress().toLowerCase(), asset);
            }
        }
        String heightHex = EvmCodec.toHexQuantity(height);
        // 原生币需要完整交易列表,否则只取区块头(拿 blockHash/blockTime)
        JsonNode block = rpc.call("eth_getBlockByNumber", heightHex, nativeAsset != null);
        if (block == null || block.isNull()) {
            throw new ChainAdapterException("区块不存在: height=" + height + ", " + chainKey().canonical());
        }
        String blockHash = block.path("hash").asText();
        Instant blockTime = Instant.ofEpochSecond(EvmCodec.hexToLong(block.path("timestamp").asText()));

        List<AssetTransferEvent> events = new ArrayList<>();
        if (nativeAsset != null) {
            extractNativeTransfers(block, nativeAsset, height, blockHash, blockTime, events);
        }
        if (!tokenByContract.isEmpty()) {
            extractTokenTransfers(heightHex, tokenByContract, height, blockHash, blockTime, events);
        }
        return events;
    }

    /**
     * 从区块交易列表提取原生币转账(value &gt; 0 且非合约创建)
     */
    private void extractNativeTransfers(JsonNode block, WatchedAsset nativeAsset, long height,
                                        String blockHash, Instant blockTime, List<AssetTransferEvent> events) {
        for (JsonNode tx : block.path("transactions")) {
            BigInteger value = EvmCodec.hexToBigInteger(tx.path("value").asText());
            JsonNode to = tx.get("to");
            if (value.signum() <= 0 || to == null || to.isNull()) {
                continue;
            }
            events.add(AssetTransferEvent.builder()
                .chainKey(chainKey())
                .assetId(nativeAsset.getAssetId())
                .txHash(tx.path("hash").asText())
                .eventIndex(AssetTransferEvent.NATIVE_EVENT_INDEX)
                .height(height)
                .blockHash(blockHash)
                .blockTime(blockTime)
                .fromAddress(tx.path("from").asText().toLowerCase())
                .toAddress(to.asText().toLowerCase())
                .rawAmount(value)
                .build());
        }
    }

    /**
     * 从 Transfer 事件日志提取受关注合约的 ERC-20 转账
     */
    private void extractTokenTransfers(String heightHex, Map<String, WatchedAsset> tokenByContract, long height,
                                       String blockHash, Instant blockTime, List<AssetTransferEvent> events) {
        Map<String, Object> filter = new HashMap<>();
        filter.put("fromBlock", heightHex);
        filter.put("toBlock", heightHex);
        filter.put("topics", List.of(TRANSFER_TOPIC));
        JsonNode logs = rpc.call("eth_getLogs", filter);
        for (JsonNode log : logs) {
            WatchedAsset asset = tokenByContract.get(log.path("address").asText().toLowerCase());
            JsonNode topics = log.path("topics");
            // ERC-20 Transfer 为 3 个 topic;4 个 topic 是 ERC-721(tokenId indexed),排除
            if (asset == null || topics.size() != 3) {
                continue;
            }
            events.add(AssetTransferEvent.builder()
                .chainKey(chainKey())
                .assetId(asset.getAssetId())
                .txHash(log.path("transactionHash").asText())
                .eventIndex((int) EvmCodec.hexToLong(log.path("logIndex").asText()))
                .height(height)
                .blockHash(blockHash)
                .blockTime(blockTime)
                .fromAddress(EvmCodec.topicToAddress(topics.get(1).asText()))
                .toAddress(EvmCodec.topicToAddress(topics.get(2).asText()))
                .rawAmount(EvmCodec.hexToBigInteger(log.path("data").asText()))
                .build());
        }
    }

    @Override
    public TxStatus getTxStatus(String txHash) {
        JsonNode receipt = rpc.call("eth_getTransactionReceipt", txHash);
        if (receipt != null && !receipt.isNull()) {
            return BigInteger.ONE.equals(EvmCodec.hexToBigInteger(receipt.path("status").asText()))
                ? TxStatus.SUCCESS : TxStatus.FAILED;
        }
        JsonNode tx = rpc.call("eth_getTransactionByHash", txHash);
        return (tx != null && !tx.isNull()) ? TxStatus.PENDING : TxStatus.NOT_FOUND;
    }

}
