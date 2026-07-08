package org.dromara.chain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.chain.api.enums.AssetType;
import org.dromara.chain.api.enums.ProtocolType;
import org.dromara.chain.api.model.ChainKey;
import org.dromara.chain.api.model.WatchedAsset;
import org.dromara.chain.domain.ChainAsset;
import org.dromara.chain.domain.bo.ChainAssetBo;
import org.dromara.chain.domain.vo.ChainAssetVo;
import org.dromara.chain.mapper.ChainAssetMapper;
import org.dromara.chain.service.IChainAssetService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 链资产配置 服务层实现
 *
 * @author jarvey
 */
@RequiredArgsConstructor
@Service
public class ChainAssetServiceImpl implements IChainAssetService {

    /**
     * 状态-正常
     */
    private static final String STATUS_NORMAL = "0";

    private final ChainAssetMapper assetMapper;

    @Override
    public PageResult<ChainAssetVo> selectPageAssetList(ChainAssetBo bo, PageQuery pageQuery) {
        Page<ChainAssetVo> page = assetMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public ChainAssetVo selectAssetById(Long assetId) {
        return assetMapper.selectVoById(assetId);
    }

    @Override
    public int insertAsset(ChainAssetBo bo) {
        validateBo(bo);
        checkUnique(bo);
        ChainAsset asset = MapstructUtils.convert(bo, ChainAsset.class);
        if (StringUtils.isBlank(asset.getStatus())) {
            asset.setStatus(STATUS_NORMAL);
        }
        int rows = assetMapper.insert(asset);
        bo.setAssetId(asset.getAssetId());
        return rows;
    }

    @Override
    public int updateAsset(ChainAssetBo bo) {
        validateBo(bo);
        checkUnique(bo);
        ChainAsset asset = MapstructUtils.convert(bo, ChainAsset.class);
        return assetMapper.updateById(asset);
    }

    @Override
    public int deleteAssetByIds(Long[] assetIds) {
        return assetMapper.deleteByIds(Arrays.asList(assetIds));
    }

    @Override
    public List<WatchedAsset> listWatched(String network) {
        List<ChainAsset> assets = assetMapper.selectList(Wrappers.lambdaQuery(ChainAsset.class)
            .eq(ChainAsset::getNetwork, network)
            .eq(ChainAsset::getStatus, STATUS_NORMAL));
        return assets.stream().map(this::toWatched).toList();
    }

    @Override
    public WatchedAsset getWatched(Long assetId) {
        ChainAsset asset = assetMapper.selectById(assetId);
        if (asset == null) {
            throw new ServiceException("资产不存在: " + assetId);
        }
        if (!STATUS_NORMAL.equals(asset.getStatus())) {
            throw new ServiceException("资产已停用: " + asset.getSymbol());
        }
        return toWatched(asset);
    }

    /**
     * 实体转扫块/转账用的受关注资产口径
     */
    private WatchedAsset toWatched(ChainAsset asset) {
        return WatchedAsset.builder()
            .assetId(asset.getAssetId())
            .chainKey(ChainKey.of(ProtocolType.valueOf(asset.getProtocol()), asset.getNetwork()))
            .assetType(AssetType.valueOf(asset.getAssetType()))
            .symbol(asset.getSymbol())
            .contractAddress(asset.getContractAddress())
            .decimals(asset.getDecimals() != null ? asset.getDecimals() : 0)
            .build();
    }

    /**
     * 校验协议/资产类型枚举合法,TOKEN 必须带合约标识
     */
    private void validateBo(ChainAssetBo bo) {
        AssetType assetType;
        try {
            ProtocolType.valueOf(bo.getProtocol());
            assetType = AssetType.valueOf(bo.getAssetType());
        } catch (IllegalArgumentException e) {
            throw new ServiceException("链协议或资产类型不合法: " + bo.getProtocol() + "/" + bo.getAssetType());
        }
        if (assetType == AssetType.TOKEN && StringUtils.isBlank(bo.getContractAddress())) {
            throw new ServiceException("合约代币必须填写合约标识");
        }
    }

    /**
     * 唯一性校验:同网络下原生币仅一条,代币按合约标识唯一
     */
    private void checkUnique(ChainAssetBo bo) {
        LambdaQueryWrapper<ChainAsset> lqw = Wrappers.lambdaQuery(ChainAsset.class)
            .eq(ChainAsset::getNetwork, bo.getNetwork())
            .eq(ChainAsset::getAssetType, bo.getAssetType())
            .ne(bo.getAssetId() != null, ChainAsset::getAssetId, bo.getAssetId());
        if (AssetType.TOKEN.name().equals(bo.getAssetType())) {
            lqw.eq(ChainAsset::getContractAddress, bo.getContractAddress());
        }
        if (assetMapper.exists(lqw)) {
            throw new ServiceException("同网络下已存在相同资产配置");
        }
    }

    /**
     * 构造资产列表查询条件
     */
    private LambdaQueryWrapper<ChainAsset> buildQueryWrapper(ChainAssetBo bo) {
        return Wrappers.lambdaQuery(ChainAsset.class)
            .eq(StringUtils.isNotBlank(bo.getProtocol()), ChainAsset::getProtocol, bo.getProtocol())
            .eq(StringUtils.isNotBlank(bo.getNetwork()), ChainAsset::getNetwork, bo.getNetwork())
            .eq(StringUtils.isNotBlank(bo.getAssetType()), ChainAsset::getAssetType, bo.getAssetType())
            .like(StringUtils.isNotBlank(bo.getSymbol()), ChainAsset::getSymbol, bo.getSymbol())
            .eq(StringUtils.isNotBlank(bo.getStatus()), ChainAsset::getStatus, bo.getStatus())
            .orderByAsc(ChainAsset::getAssetId);
    }

}
