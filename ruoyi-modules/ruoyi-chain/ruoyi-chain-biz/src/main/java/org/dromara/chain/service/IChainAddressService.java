package org.dromara.chain.service;

import org.dromara.chain.domain.bo.ChainAddressBo;
import org.dromara.chain.domain.vo.ChainAddressVo;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.Map;

/**
 * 链托管地址 服务层
 *
 * @author jarvey
 */
public interface IChainAddressService {

    /**
     * 分页查询托管地址
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 地址分页结果
     */
    PageResult<ChainAddressVo> selectPageAddressList(ChainAddressBo bo, PageQuery pageQuery);

    /**
     * 查询地址详情
     *
     * @param addressId 地址ID
     * @return 地址详情
     */
    ChainAddressVo selectAddressById(Long addressId);

    /**
     * 新增托管地址
     *
     * @param bo 地址参数
     * @return 影响行数
     */
    int insertAddress(ChainAddressBo bo);

    /**
     * 修改托管地址
     *
     * @param bo 地址参数
     * @return 影响行数
     */
    int updateAddress(ChainAddressBo bo);

    /**
     * 批量删除托管地址
     *
     * @param addressIds 地址ID集合
     * @return 影响行数
     */
    int deleteAddressByIds(Long[] addressIds);

    /**
     * 查询指定网络下状态正常的充值地址(小写地址 → 地址ID,供扫块归属匹配)
     *
     * @param network 网络名
     * @return 小写地址到地址ID的映射
     */
    Map<String, Long> mapDepositAddresses(String network);

}
