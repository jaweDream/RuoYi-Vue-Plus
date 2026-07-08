package org.dromara.chain.api.enums;

/**
 * 链协议类型
 *
 * <p>协议是适配器的划分单位:同一协议下可存在多个网络(如 EVM 协议下的
 * eth-mainnet / bsc / polygon),它们共用同一套适配器实现,仅 RPC 端点与链参数不同。
 *
 * @author jarvey
 */
public enum ProtocolType {

    /**
     * 以太坊虚拟机协议(以太坊及全部 EVM 兼容链)
     */
    EVM,

    /**
     * Solana 协议
     */
    SOLANA,

    /**
     * Aptos 协议
     */
    APTOS

}
