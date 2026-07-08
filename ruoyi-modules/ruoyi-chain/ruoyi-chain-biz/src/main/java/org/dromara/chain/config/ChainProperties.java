package org.dromara.chain.config;

import lombok.Data;
import org.dromara.chain.api.enums.ProtocolType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 链资产模块配置
 *
 * <p>配置示例:
 * <pre>{@code
 * chain:
 *   enabled: true
 *   networks:
 *     eth-mainnet:
 *       protocol: EVM
 *       rpc-url: https://eth.example.com/rpc
 *       confirmations: 12
 *       scan-batch-size: 20
 * }</pre>
 *
 * @author jarvey
 */
@Data
@ConfigurationProperties(prefix = "chain")
public class ChainProperties {

    /**
     * 模块总开关(关闭时扫块/状态刷新任务空转,接口仍可管理配置)
     */
    private boolean enabled = false;

    /**
     * 网络接入配置,key 为网络名(全局唯一,即业务表 network 列取值)
     */
    private Map<String, NetworkProps> networks = new LinkedHashMap<>();

    /**
     * 单个网络的接入配置
     */
    @Data
    public static class NetworkProps {

        /**
         * 链协议(EVM/SOLANA/APTOS),决定使用哪个适配器
         */
        private ProtocolType protocol;

        /**
         * 节点 RPC 端点
         */
        private String rpcUrl;

        /**
         * 是否启用该网络
         */
        private boolean enabled = true;

        /**
         * 入账确认深度覆盖值(空则用适配器按协议给出的默认值)
         */
        private Integer confirmations;

        /**
         * 单次扫块任务最多推进的块数(控制单次任务时长)
         */
        private int scanBatchSize = 20;

        /**
         * 首次建游标的起始高度(空则从当前链头开始,不回扫历史)
         */
        private Long startHeight;

        /**
         * 协议相关扩展参数(原样传给适配器)
         */
        private Map<String, String> options = new HashMap<>();

    }

}
