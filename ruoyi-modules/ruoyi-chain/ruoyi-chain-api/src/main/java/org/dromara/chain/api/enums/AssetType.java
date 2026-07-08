package org.dromara.chain.api.enums;

/**
 * 链上资产类型
 *
 * @author jarvey
 */
public enum AssetType {

    /**
     * 原生币(ETH/SOL/APT),转账即链原生价值转移,无合约地址
     */
    NATIVE,

    /**
     * 合约代币(EVM 的 ERC-20、Solana 的 SPL Token、Aptos 的 Coin/FA),
     * 以 {@code contractAddress} 标识(合约地址 / mint 地址 / FA metadata 地址)
     */
    TOKEN

}
