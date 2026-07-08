package org.dromara.chain.evm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.chain.api.enums.AssetType;
import org.dromara.chain.api.exception.ChainAdapterException;
import org.dromara.chain.api.model.BroadcastResult;
import org.dromara.chain.api.model.ChainKey;
import org.dromara.chain.api.model.ChainNetworkConfig;
import org.dromara.chain.api.model.FeeEstimate;
import org.dromara.chain.api.model.PreparedTx;
import org.dromara.chain.api.model.SignedTx;
import org.dromara.chain.api.model.TransferRequest;
import org.dromara.chain.api.spi.ChainWriter;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * EVM 链写实现
 *
 * <p>估费、nonce 获取、交易字段组装与广播为真实实现;RLP 编码与签名不在此处——
 * {@link #buildTransfer} 产出字段级 payload(legacy 交易字段 JSON),由同协议的
 * {@code ChainSigner} 实现(如 web3j/KMS)完成编码签名后回传 raw hex 再广播。
 *
 * @author jarvey
 */
public class EvmChainWriter implements ChainWriter {

    /**
     * ERC-20 transfer(address,uint256) 方法选择器
     */
    private static final String TRANSFER_SELECTOR = "0xa9059cbb";

    /**
     * 原生币转账固定 gas 消耗
     */
    private static final BigInteger NATIVE_GAS_LIMIT = BigInteger.valueOf(21_000);

    /**
     * 代币转账估气失败时的兜底 gasLimit
     */
    private static final BigInteger TOKEN_GAS_LIMIT_FALLBACK = BigInteger.valueOf(100_000);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ChainNetworkConfig config;
    private final EvmJsonRpcClient rpc;

    public EvmChainWriter(ChainNetworkConfig config, EvmJsonRpcClient rpc) {
        this.config = config;
        this.rpc = rpc;
    }

    @Override
    public ChainKey chainKey() {
        return config.getChainKey();
    }

    @Override
    public FeeEstimate estimateFee(TransferRequest request) {
        BigInteger gasPrice = EvmCodec.hexToBigInteger(rpc.call("eth_gasPrice").asText());
        GasPlan plan = planGas(request);
        Map<String, String> details = new HashMap<>();
        details.put("gasPrice", gasPrice.toString());
        details.put("gasLimit", plan.gasLimit.toString());
        details.put("gasLimitSource", plan.source);
        return FeeEstimate.builder()
            .rawFee(gasPrice.multiply(plan.gasLimit))
            .feeSymbol(config.getOptions().getOrDefault("nativeSymbol", "ETH"))
            .details(details)
            .build();
    }

    @Override
    public PreparedTx buildTransfer(TransferRequest request) {
        BigInteger nonce = EvmCodec.hexToBigInteger(
            rpc.call("eth_getTransactionCount", request.getFromAddress(), "pending").asText());
        long chainId = EvmCodec.hexToLong(rpc.call("eth_chainId").asText());
        BigInteger gasPrice = EvmCodec.hexToBigInteger(rpc.call("eth_gasPrice").asText());
        GasPlan plan = planGas(request);

        // legacy 交易字段;签名器如需 EIP-1559 可基于 meta 自行改写费用字段
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("type", "legacy");
        meta.put("chainId", String.valueOf(chainId));
        meta.put("nonce", nonce.toString());
        meta.put("gasPrice", gasPrice.toString());
        meta.put("gasLimit", plan.gasLimit.toString());
        if (request.getAsset().getAssetType() == AssetType.NATIVE) {
            meta.put("to", request.getToAddress());
            meta.put("value", request.getRawAmount().toString());
            meta.put("data", "0x");
        } else {
            meta.put("to", request.getAsset().getContractAddress());
            meta.put("value", "0");
            meta.put("data", transferData(request.getToAddress(), request.getRawAmount()));
        }
        try {
            return PreparedTx.builder()
                .chainKey(chainKey())
                .payload(MAPPER.writeValueAsString(meta))
                .meta(meta)
                .build();
        } catch (JsonProcessingException e) {
            throw new ChainAdapterException("组装交易 payload 序列化失败", e);
        }
    }

    @Override
    public BroadcastResult broadcast(SignedTx tx) {
        try {
            String txHash = rpc.call("eth_sendRawTransaction", tx.getRawSigned()).asText();
            return BroadcastResult.builder().accepted(true).txHash(txHash).build();
        } catch (ChainAdapterException e) {
            // 节点拒绝(nonce 冲突、余额不足、gas 过低等)以结果形式返回,由业务层入库置败
            return BroadcastResult.builder().accepted(false).message(e.getMessage()).build();
        }
    }

    /**
     * 计算 gasLimit:原生币固定 21000;代币走 eth_estimateGas,失败时用兜底值
     */
    private GasPlan planGas(TransferRequest request) {
        if (request.getAsset().getAssetType() == AssetType.NATIVE) {
            return new GasPlan(NATIVE_GAS_LIMIT, "fixed");
        }
        Map<String, Object> callTx = new HashMap<>();
        callTx.put("from", request.getFromAddress());
        callTx.put("to", request.getAsset().getContractAddress());
        callTx.put("data", transferData(request.getToAddress(), request.getRawAmount()));
        try {
            BigInteger estimated = EvmCodec.hexToBigInteger(rpc.call("eth_estimateGas", callTx).asText());
            return new GasPlan(estimated, "estimated");
        } catch (ChainAdapterException e) {
            return new GasPlan(TOKEN_GAS_LIMIT_FALLBACK, "fallback");
        }
    }

    /**
     * 构造 ERC-20 transfer 调用数据
     */
    private String transferData(String toAddress, BigInteger rawAmount) {
        return TRANSFER_SELECTOR + EvmCodec.padAddress(toAddress) + EvmCodec.padUint(rawAmount);
    }

    private record GasPlan(BigInteger gasLimit, String source) {
    }

}
