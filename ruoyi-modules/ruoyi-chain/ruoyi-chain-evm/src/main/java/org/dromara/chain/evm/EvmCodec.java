package org.dromara.chain.evm;

import java.math.BigInteger;

/**
 * EVM hex 编解码工具(0x 前缀数量值与 32 字节 topic/参数的互转)
 *
 * @author jarvey
 */
final class EvmCodec {

    private EvmCodec() {
    }

    /**
     * 解析 0x 前缀 hex 数量值为 long
     *
     * @param hex 形如 0x1a 的数量值
     * @return long 值
     */
    static long hexToLong(String hex) {
        return hexToBigInteger(hex).longValueExact();
    }

    /**
     * 解析 0x 前缀 hex 数量值为 BigInteger(空值/0x 视为 0)
     *
     * @param hex 形如 0x1a 的数量值
     * @return BigInteger 值
     */
    static BigInteger hexToBigInteger(String hex) {
        if (hex == null || hex.isEmpty() || "0x".equals(hex)) {
            return BigInteger.ZERO;
        }
        String body = hex.startsWith("0x") ? hex.substring(2) : hex;
        if (body.isEmpty()) {
            return BigInteger.ZERO;
        }
        return new BigInteger(body, 16);
    }

    /**
     * long 转 0x 前缀 hex 数量值
     *
     * @param value 数量值
     * @return 形如 0x1a 的 hex
     */
    static String toHexQuantity(long value) {
        return "0x" + Long.toHexString(value);
    }

    /**
     * 32 字节 topic 还原为 20 字节地址(取低 20 字节)
     *
     * @param topic 64 位 hex 的 topic
     * @return 0x 前缀小写地址
     */
    static String topicToAddress(String topic) {
        String body = topic.startsWith("0x") ? topic.substring(2) : topic;
        return "0x" + body.substring(body.length() - 40).toLowerCase();
    }

    /**
     * 地址左填充为 32 字节 ABI 参数
     *
     * @param address 0x 前缀地址
     * @return 64 位 hex(不带 0x)
     */
    static String padAddress(String address) {
        String body = address.startsWith("0x") ? address.substring(2) : address;
        return "0".repeat(64 - body.length()) + body.toLowerCase();
    }

    /**
     * 无符号整数左填充为 32 字节 ABI 参数
     *
     * @param value 非负整数
     * @return 64 位 hex(不带 0x)
     */
    static String padUint(BigInteger value) {
        String body = value.toString(16);
        return "0".repeat(64 - body.length()) + body;
    }

}
