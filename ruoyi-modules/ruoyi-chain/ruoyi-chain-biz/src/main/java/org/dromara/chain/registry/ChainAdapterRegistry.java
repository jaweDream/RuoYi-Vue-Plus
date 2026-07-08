package org.dromara.chain.registry;

import org.dromara.chain.api.enums.ProtocolType;
import org.dromara.chain.api.model.ChainKey;
import org.dromara.chain.api.model.ChainNetworkConfig;
import org.dromara.chain.api.spi.ChainClientFactory;
import org.dromara.chain.api.spi.ChainReader;
import org.dromara.chain.api.spi.ChainWriter;
import org.dromara.chain.config.ChainProperties;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 链适配器注册中心(网络名 → Reader/Writer 实例的唯一入口)
 *
 * <p>启动时收集各适配器模块注册的 {@link ChainClientFactory},按 {@code chain.networks}
 * 配置在首次使用时创建并缓存读写实例;业务代码一律经由本类取适配器,不直接触碰工厂。
 *
 * @author jarvey
 */
@Component
public class ChainAdapterRegistry {

    private final ChainProperties properties;
    private final Map<ProtocolType, ChainClientFactory> factories = new EnumMap<>(ProtocolType.class);
    private final Map<String, ChainReader> readers = new ConcurrentHashMap<>();
    private final Map<String, ChainWriter> writers = new ConcurrentHashMap<>();

    public ChainAdapterRegistry(ChainProperties properties, List<ChainClientFactory> factoryList) {
        this.properties = properties;
        for (ChainClientFactory factory : factoryList) {
            factories.put(factory.protocol(), factory);
        }
    }

    /**
     * 已启用网络的链标识列表(按配置顺序)
     *
     * @return 链网络标识列表
     */
    public List<ChainKey> enabledNetworks() {
        List<ChainKey> keys = new ArrayList<>();
        properties.getNetworks().forEach((network, props) -> {
            if (props.isEnabled()) {
                keys.add(ChainKey.of(props.getProtocol(), network));
            }
        });
        return keys;
    }

    /**
     * 取网络配置,不存在或未启用时抛业务异常
     *
     * @param network 网络名
     * @return 网络配置
     */
    public ChainProperties.NetworkProps getNetworkProps(String network) {
        ChainProperties.NetworkProps props = properties.getNetworks().get(network);
        if (props == null) {
            throw new ServiceException("链网络未配置: " + network + "(检查 chain.networks 配置)");
        }
        if (!props.isEnabled()) {
            throw new ServiceException("链网络已停用: " + network);
        }
        if (props.getProtocol() == null || StringUtils.isBlank(props.getRpcUrl())) {
            throw new ServiceException("链网络配置不完整(protocol/rpc-url 必填): " + network);
        }
        return props;
    }

    /**
     * 取指定网络的读实例(懒创建并缓存)
     *
     * @param network 网络名
     * @return 链读实例
     */
    public ChainReader getReader(String network) {
        return readers.computeIfAbsent(network, k -> factoryOf(k).createReader(toConfig(k)));
    }

    /**
     * 取指定网络的写实例(懒创建并缓存)
     *
     * @param network 网络名
     * @return 链写实例
     */
    public ChainWriter getWriter(String network) {
        return writers.computeIfAbsent(network, k -> factoryOf(k).createWriter(toConfig(k)));
    }

    /**
     * 按网络协议定位适配器工厂
     */
    private ChainClientFactory factoryOf(String network) {
        ChainProperties.NetworkProps props = getNetworkProps(network);
        ChainClientFactory factory = factories.get(props.getProtocol());
        if (factory == null) {
            throw new ServiceException("缺少协议适配器: " + props.getProtocol()
                + "(确认对应 ruoyi-chain-* 适配器模块已引入)");
        }
        return factory;
    }

    /**
     * 配置转适配器入参
     */
    private ChainNetworkConfig toConfig(String network) {
        ChainProperties.NetworkProps props = getNetworkProps(network);
        return ChainNetworkConfig.builder()
            .chainKey(ChainKey.of(props.getProtocol(), network))
            .rpcUrl(props.getRpcUrl())
            .confirmations(props.getConfirmations())
            .options(props.getOptions())
            .build();
    }

}
