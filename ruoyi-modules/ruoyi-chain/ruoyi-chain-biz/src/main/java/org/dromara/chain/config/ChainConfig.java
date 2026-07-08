package org.dromara.chain.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 链资产模块装配
 *
 * @author jarvey
 */
@Configuration
@EnableConfigurationProperties(ChainProperties.class)
public class ChainConfig {

}
