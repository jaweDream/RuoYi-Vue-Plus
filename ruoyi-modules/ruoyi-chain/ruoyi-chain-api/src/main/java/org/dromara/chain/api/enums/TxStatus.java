package org.dromara.chain.api.enums;

/**
 * 链上交易状态(归一化后的查询结果)
 *
 * @author jarvey
 */
public enum TxStatus {

    /**
     * 已被节点接收但尚未打包,或已打包但未达确认深度
     */
    PENDING,

    /**
     * 已上链且执行成功
     */
    SUCCESS,

    /**
     * 已上链但执行失败(如 EVM revert),手续费已消耗
     */
    FAILED,

    /**
     * 节点查询不到该交易(未广播成功、已被替换或被节点丢弃)
     */
    NOT_FOUND

}
