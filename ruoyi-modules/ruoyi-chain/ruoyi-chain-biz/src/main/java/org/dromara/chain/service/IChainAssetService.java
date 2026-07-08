package org.dromara.chain.service;

import org.dromara.chain.api.model.WatchedAsset;
import org.dromara.chain.domain.bo.ChainAssetBo;
import org.dromara.chain.domain.vo.ChainAssetVo;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.List;

/**
 * 链资产配置 服务层
 *
 * @author jarvey
 */
public interface IChainAssetService {

    /**
     * 分页查询资产配置
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 资产分页结果
     */
    PageResult<ChainAssetVo> selectPageAssetList(ChainAssetBo bo, PageQuery pageQuery);

    /**
     * 查询资产详情
     *
     * @param assetId 资产ID
     * @return 资产详情
     */
    ChainAssetVo selectAssetById(Long assetId);

    /**
     * 新增资产配置
     *
     * @param bo 资产参数
     * @return 影响行数
     */
    int insertAsset(ChainAssetBo bo);

    /**
     * 修改资产配置
     *
     * @param bo 资产参数
     * @return 影响行数
     */
    int updateAsset(ChainAssetBo bo);

    /**
     * 批量删除资产配置
     *
     * @param assetIds 资产ID集合
     * @return 影响行数
     */
    int deleteAssetByIds(Long[] assetIds);

    /**
     * 查询指定网络下状态正常的受关注资产(扫块口径)
     *
     * @param network 网络名
     * @return 受关注资产列表
     */
    List<WatchedAsset> listWatched(String network);

    /**
     * 按资产ID取受关注资产口径(校验存在且状态正常)
     *
     * @param assetId 资产ID
     * @return 受关注资产
     */
    WatchedAsset getWatched(Long assetId);

}
