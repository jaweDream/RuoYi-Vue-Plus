package org.dromara.chain.api.spi;

import org.dromara.chain.api.model.BroadcastResult;
import org.dromara.chain.api.model.ChainKey;
import org.dromara.chain.api.model.FeeEstimate;
import org.dromara.chain.api.model.PreparedTx;
import org.dromara.chain.api.model.SignedTx;
import org.dromara.chain.api.model.TransferRequest;

/**
 * 链写能力 SPI(面向单个网络的交易原语)
 *
 * <p>刻意不包含签名:签名涉及私钥托管,由 {@link ChainSigner} 单独抽象,
 * 业务层负责 组交易 → 签名 → 广播 的编排与状态机,适配器只做协议翻译。
 *
 * @author jarvey
 */
public interface ChainWriter {

    /**
     * 本实例服务的链网络
     *
     * @return 链网络标识
     */
    ChainKey chainKey();

    /**
     * 估算转账手续费
     *
     * @param request 转账请求
     * @return 手续费估算
     */
    FeeEstimate estimateFee(TransferRequest request);

    /**
     * 组装待签名交易(内部处理 nonce/sequence/blockhash 等协议细节)
     *
     * @param request 转账请求
     * @return 待签名交易
     */
    PreparedTx buildTransfer(TransferRequest request);

    /**
     * 广播已签名交易
     *
     * @param tx 已签名交易
     * @return 广播结果(节点拒绝时 accepted=false,不抛异常)
     */
    BroadcastResult broadcast(SignedTx tx);

}
