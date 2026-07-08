package org.dromara.chain.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.chain.domain.bo.ChainCursorResetBo;
import org.dromara.chain.domain.bo.ChainDepositRecordBo;
import org.dromara.chain.domain.vo.ChainDepositRecordVo;
import org.dromara.chain.domain.vo.ChainScanCursorVo;
import org.dromara.chain.service.IChainScanService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 链扫块监控(游标运维 + 充值记录)
 *
 * @author jarvey
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/chain/scan")
public class ChainScanController extends BaseController {

    private final IChainScanService scanService;

    /**
     * 查询全部网络的扫块游标
     *
     * @return 游标列表
     */
    @SaCheckPermission("chain:scan:list")
    @GetMapping("/cursor/list")
    public R<List<ChainScanCursorVo>> cursorList() {
        return R.ok(scanService.listCursors());
    }

    /**
     * 重置扫块游标高度(回扫/跳块运维操作,充值入库有唯一键保证回扫幂等)
     *
     * @param bo 重置参数
     * @return 操作结果
     */
    @SaCheckPermission("chain:scan:edit")
    @Log(title = "扫块游标", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/cursor/reset")
    public R<Void> resetCursor(@Validated @RequestBody ChainCursorResetBo bo) {
        return toAjax(scanService.resetCursor(bo));
    }

    /**
     * 暂停指定网络扫块
     *
     * @param network 网络名
     * @return 操作结果
     */
    @SaCheckPermission("chain:scan:edit")
    @Log(title = "扫块游标", businessType = BusinessType.UPDATE)
    @PutMapping("/cursor/pause/{network}")
    public R<Void> pauseCursor(@PathVariable String network) {
        return toAjax(scanService.pauseCursor(network));
    }

    /**
     * 恢复指定网络扫块
     *
     * @param network 网络名
     * @return 操作结果
     */
    @SaCheckPermission("chain:scan:edit")
    @Log(title = "扫块游标", businessType = BusinessType.UPDATE)
    @PutMapping("/cursor/resume/{network}")
    public R<Void> resumeCursor(@PathVariable String network) {
        return toAjax(scanService.resumeCursor(network));
    }

    /**
     * 分页查询充值记录
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 充值记录分页结果
     */
    @SaCheckPermission("chain:deposit:list")
    @GetMapping("/deposit/list")
    public R<PageResult<ChainDepositRecordVo>> depositList(ChainDepositRecordBo bo, PageQuery pageQuery) {
        return R.ok(scanService.selectPageDepositList(bo, pageQuery));
    }

}
