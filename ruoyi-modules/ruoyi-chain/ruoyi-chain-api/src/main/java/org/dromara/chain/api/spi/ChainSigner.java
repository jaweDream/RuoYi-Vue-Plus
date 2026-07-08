package org.dromara.chain.api.spi;

import org.dromara.chain.api.enums.ProtocolType;
import org.dromara.chain.api.model.PreparedTx;
import org.dromara.chain.api.model.SignedTx;

/**
 * 交易签名 SPI(按协议注册,与私钥托管方案解耦)
 *
 * <p>本仓库不内置任何持有私钥的实现;生产接入方按托管方案自行实现
 * (如 KMS/HSM、MPC 服务、web3j 本地签名等)并注册为 Spring Bean。
 * 未注册签名器时,转账任务会在签名环节明确失败,而不是静默跳过。
 *
 * @author jarvey
 */
public interface ChainSigner {

    /**
     * 本签名器支持的协议
     *
     * @return 协议类型
     */
    ProtocolType protocol();

    /**
     * 对待签名交易签名
     *
     * @param tx     待签名交易(payload/meta 格式与同协议适配器约定一致)
     * @param keyRef 密钥引用(通常为出账地址,由实现方映射到实际密钥)
     * @return 已签名交易
     */
    SignedTx sign(PreparedTx tx, String keyRef);

}
