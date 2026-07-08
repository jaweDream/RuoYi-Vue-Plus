package org.dromara.chain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 链转账任务状态机
 *
 * <p>状态流转:待执行 → 执行中 → 已广播 → 成功/失败;失败可重试(→ 待执行)。
 * "执行中"由条件更新抢占,防止并发重复出账。
 *
 * @author jarvey
 */
@Getter
@AllArgsConstructor
public enum ChainTransferStatus {

    /**
     * 待执行(已建单,未组交易)
     */
    WAITING("0", "待执行"),

    /**
     * 执行中(已抢占,组交易/签名/广播进行中)
     */
    EXECUTING("1", "执行中"),

    /**
     * 已广播(等待链上确认)
     */
    BROADCASTED("2", "已广播"),

    /**
     * 链上执行成功
     */
    SUCCESS("3", "成功"),

    /**
     * 失败(组交易/签名/广播失败,或链上执行失败),可重试
     */
    FAILED("4", "失败");

    /**
     * 存库状态码
     */
    private final String code;

    /**
     * 状态说明
     */
    private final String info;

}
