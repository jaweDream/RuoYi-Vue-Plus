package org.dromara.chain.api.enums;

/**
 * 链终局性模型
 *
 * <p>决定扫块编排是否需要预留确认深度:概率终局链必须落后链头若干确认再入账,
 * 确定终局链在承诺级别达到后即可视为不可回滚。
 *
 * @author jarvey
 */
public enum FinalityType {

    /**
     * 概率终局(如 EVM PoW/PoS 链),存在区块重组可能,需按确认数预留安全深度
     */
    PROBABILISTIC,

    /**
     * 确定终局(如 Solana finalized 承诺级别、Aptos),达到终局后不可回滚
     */
    DETERMINISTIC

}
