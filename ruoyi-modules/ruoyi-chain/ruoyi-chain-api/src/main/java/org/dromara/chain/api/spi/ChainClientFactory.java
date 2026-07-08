package org.dromara.chain.api.spi;

import org.dromara.chain.api.enums.ProtocolType;
import org.dromara.chain.api.model.ChainNetworkConfig;

/**
 * 链适配器工厂 SPI(协议 → 读写实例)
 *
 * <p>每个协议适配器模块注册一个工厂 Bean;业务层按网络配置调用工厂创建
 * Reader/Writer 实例并缓存(一个网络一对实例)。新增协议 = 新增一个适配器模块,
 * 业务层零改动。
 *
 * @author jarvey
 */
public interface ChainClientFactory {

    /**
     * 本工厂支持的协议
     *
     * @return 协议类型
     */
    ProtocolType protocol();

    /**
     * 创建指定网络的读实例
     *
     * @param config 网络接入配置
     * @return 链读实例
     */
    ChainReader createReader(ChainNetworkConfig config);

    /**
     * 创建指定网络的写实例
     *
     * @param config 网络接入配置
     * @return 链写实例
     */
    ChainWriter createWriter(ChainNetworkConfig config);

}
