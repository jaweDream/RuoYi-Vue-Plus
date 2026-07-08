package org.dromara.chain.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.chain.domain.bo.ChainAssetBo;
import org.dromara.chain.domain.vo.ChainAssetVo;
import org.dromara.chain.service.IChainAssetService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 链资产配置
 *
 * @author jarvey
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/chain/asset")
public class ChainAssetController extends BaseController {

    private final IChainAssetService assetService;

    /**
     * 分页查询链资产配置列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 资产分页结果
     */
    @SaCheckPermission("chain:asset:list")
    @GetMapping("/list")
    public R<PageResult<ChainAssetVo>> list(ChainAssetBo bo, PageQuery pageQuery) {
        return R.ok(assetService.selectPageAssetList(bo, pageQuery));
    }

    /**
     * 获取链资产配置详情
     *
     * @param assetId 资产ID
     * @return 资产详情
     */
    @SaCheckPermission("chain:asset:query")
    @GetMapping("/{assetId}")
    public R<ChainAssetVo> getInfo(@PathVariable Long assetId) {
        return R.ok(assetService.selectAssetById(assetId));
    }

    /**
     * 新增链资产配置
     *
     * @param bo 资产参数
     * @return 操作结果
     */
    @SaCheckPermission("chain:asset:add")
    @Log(title = "链资产配置", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated @RequestBody ChainAssetBo bo) {
        return toAjax(assetService.insertAsset(bo));
    }

    /**
     * 修改链资产配置
     *
     * @param bo 资产参数
     * @return 操作结果
     */
    @SaCheckPermission("chain:asset:edit")
    @Log(title = "链资产配置", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated @RequestBody ChainAssetBo bo) {
        return toAjax(assetService.updateAsset(bo));
    }

    /**
     * 删除链资产配置
     *
     * @param assetIds 资产ID串
     * @return 操作结果
     */
    @SaCheckPermission("chain:asset:remove")
    @Log(title = "链资产配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{assetIds}")
    public R<Void> remove(@PathVariable Long[] assetIds) {
        return toAjax(assetService.deleteAssetByIds(assetIds));
    }

}
