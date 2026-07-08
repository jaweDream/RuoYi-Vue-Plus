package org.dromara.chain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.chain.api.enums.ProtocolType;
import org.dromara.chain.domain.ChainAddress;
import org.dromara.chain.domain.bo.ChainAddressBo;
import org.dromara.chain.domain.vo.ChainAddressVo;
import org.dromara.chain.mapper.ChainAddressMapper;
import org.dromara.chain.service.IChainAddressService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 链托管地址 服务层实现
 *
 * @author jarvey
 */
@RequiredArgsConstructor
@Service
public class ChainAddressServiceImpl implements IChainAddressService {

    /**
     * 状态-正常
     */
    private static final String STATUS_NORMAL = "0";

    /**
     * 地址类型-充值地址
     */
    private static final String TYPE_DEPOSIT = "0";

    private final ChainAddressMapper addressMapper;

    @Override
    public PageResult<ChainAddressVo> selectPageAddressList(ChainAddressBo bo, PageQuery pageQuery) {
        Page<ChainAddressVo> page = addressMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public ChainAddressVo selectAddressById(Long addressId) {
        return addressMapper.selectVoById(addressId);
    }

    @Override
    public int insertAddress(ChainAddressBo bo) {
        validateBo(bo);
        checkUnique(bo);
        ChainAddress address = MapstructUtils.convert(bo, ChainAddress.class);
        if (StringUtils.isBlank(address.getStatus())) {
            address.setStatus(STATUS_NORMAL);
        }
        int rows = addressMapper.insert(address);
        bo.setAddressId(address.getAddressId());
        return rows;
    }

    @Override
    public int updateAddress(ChainAddressBo bo) {
        validateBo(bo);
        checkUnique(bo);
        ChainAddress address = MapstructUtils.convert(bo, ChainAddress.class);
        return addressMapper.updateById(address);
    }

    @Override
    public int deleteAddressByIds(Long[] addressIds) {
        return addressMapper.deleteByIds(Arrays.asList(addressIds));
    }

    @Override
    public Map<String, Long> mapDepositAddresses(String network) {
        List<ChainAddress> list = addressMapper.selectList(Wrappers.lambdaQuery(ChainAddress.class)
            .eq(ChainAddress::getNetwork, network)
            .eq(ChainAddress::getAddressType, TYPE_DEPOSIT)
            .eq(ChainAddress::getStatus, STATUS_NORMAL));
        Map<String, Long> map = new HashMap<>(list.size());
        for (ChainAddress address : list) {
            map.put(address.getAddress().toLowerCase(), address.getAddressId());
        }
        return map;
    }

    /**
     * 校验协议枚举合法
     */
    private void validateBo(ChainAddressBo bo) {
        try {
            ProtocolType.valueOf(bo.getProtocol());
        } catch (IllegalArgumentException e) {
            throw new ServiceException("链协议不合法: " + bo.getProtocol());
        }
    }

    /**
     * 唯一性校验:同网络下地址唯一
     */
    private void checkUnique(ChainAddressBo bo) {
        boolean exists = addressMapper.exists(Wrappers.lambdaQuery(ChainAddress.class)
            .eq(ChainAddress::getNetwork, bo.getNetwork())
            .eq(ChainAddress::getAddress, bo.getAddress())
            .ne(bo.getAddressId() != null, ChainAddress::getAddressId, bo.getAddressId()));
        if (exists) {
            throw new ServiceException("同网络下该地址已登记");
        }
    }

    /**
     * 构造地址列表查询条件
     */
    private LambdaQueryWrapper<ChainAddress> buildQueryWrapper(ChainAddressBo bo) {
        return Wrappers.lambdaQuery(ChainAddress.class)
            .eq(StringUtils.isNotBlank(bo.getProtocol()), ChainAddress::getProtocol, bo.getProtocol())
            .eq(StringUtils.isNotBlank(bo.getNetwork()), ChainAddress::getNetwork, bo.getNetwork())
            .like(StringUtils.isNotBlank(bo.getAddress()), ChainAddress::getAddress, bo.getAddress())
            .eq(StringUtils.isNotBlank(bo.getAddressType()), ChainAddress::getAddressType, bo.getAddressType())
            .eq(StringUtils.isNotBlank(bo.getStatus()), ChainAddress::getStatus, bo.getStatus())
            .eq(bo.getUserId() != null, ChainAddress::getUserId, bo.getUserId())
            .orderByAsc(ChainAddress::getAddressId);
    }

}
