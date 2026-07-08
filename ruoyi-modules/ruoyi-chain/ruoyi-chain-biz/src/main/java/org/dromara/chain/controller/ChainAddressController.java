package org.dromara.chain.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.chain.domain.bo.ChainAddressBo;
import org.dromara.chain.domain.vo.ChainAddressVo;
import org.dromara.chain.service.IChainAddressService;
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
 * 链托管地址
 *
 * @author jarvey
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/chain/address")
public class ChainAddressController extends BaseController {

    private final IChainAddressService addressService;

    /**
     * 分页查询链托管地址列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 地址分页结果
     */
    @SaCheckPermission("chain:address:list")
    @GetMapping("/list")
    public R<PageResult<ChainAddressVo>> list(ChainAddressBo bo, PageQuery pageQuery) {
        return R.ok(addressService.selectPageAddressList(bo, pageQuery));
    }

    /**
     * 获取链托管地址详情
     *
     * @param addressId 地址ID
     * @return 地址详情
     */
    @SaCheckPermission("chain:address:query")
    @GetMapping("/{addressId}")
    public R<ChainAddressVo> getInfo(@PathVariable Long addressId) {
        return R.ok(addressService.selectAddressById(addressId));
    }

    /**
     * 新增链托管地址
     *
     * @param bo 地址参数
     * @return 操作结果
     */
    @SaCheckPermission("chain:address:add")
    @Log(title = "链托管地址", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated @RequestBody ChainAddressBo bo) {
        return toAjax(addressService.insertAddress(bo));
    }

    /**
     * 修改链托管地址
     *
     * @param bo 地址参数
     * @return 操作结果
     */
    @SaCheckPermission("chain:address:edit")
    @Log(title = "链托管地址", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated @RequestBody ChainAddressBo bo) {
        return toAjax(addressService.updateAddress(bo));
    }

    /**
     * 删除链托管地址
     *
     * @param addressIds 地址ID串
     * @return 操作结果
     */
    @SaCheckPermission("chain:address:remove")
    @Log(title = "链托管地址", businessType = BusinessType.DELETE)
    @DeleteMapping("/{addressIds}")
    public R<Void> remove(@PathVariable Long[] addressIds) {
        return toAjax(addressService.deleteAddressByIds(addressIds));
    }

}
