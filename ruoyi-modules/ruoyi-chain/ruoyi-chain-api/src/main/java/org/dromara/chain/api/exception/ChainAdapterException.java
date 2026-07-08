package org.dromara.chain.api.exception;

import java.io.Serial;

/**
 * 链适配器异常(RPC 通信失败、协议数据异常、能力未实现等)
 *
 * <p>业务层捕获后应记录并中止当前网络的处理(如停止推进扫块游标、置转账任务失败),
 * 不得吞掉后继续推进游标——那会造成漏账。
 *
 * @author jarvey
 */
public class ChainAdapterException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ChainAdapterException(String message) {
        super(message);
    }

    public ChainAdapterException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 能力未实现(骨架适配器占位用)
     *
     * @param feature 能力描述
     * @return 异常实例
     */
    public static ChainAdapterException unsupported(String feature) {
        return new ChainAdapterException("链适配器能力尚未实现: " + feature);
    }

}
