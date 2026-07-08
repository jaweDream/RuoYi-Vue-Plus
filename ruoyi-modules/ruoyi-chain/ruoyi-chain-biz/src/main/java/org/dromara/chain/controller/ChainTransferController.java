package org.dromara.chain.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.chain.domain.bo.ChainTransferTaskBo;
import org.dromara.chain.domain.vo.ChainTransferTaskVo;
import org.dromara.chain.service.IChainTransferService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 链转账任务(建单与执行分离:建单只登记,执行触发 估费→组交易→签名→广播)
 *
 * @author jarvey
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/chain/transfer")
public class ChainTransferController extends BaseController {

    private final IChainTransferService transferService;

    /**
     * 分页查询转账任务列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 任务分页结果
     */
    @SaCheckPermission("chain:transfer:list")
    @GetMapping("/list")
    public R<PageResult<ChainTransferTaskVo>> list(ChainTransferTaskBo bo, PageQuery pageQuery) {
        return R.ok(transferService.selectPageTaskList(bo, pageQuery));
    }

    /**
     * 获取转账任务详情
     *
     * @param taskId 任务ID
     * @return 任务详情
     */
    @SaCheckPermission("chain:transfer:query")
    @GetMapping("/{taskId}")
    public R<ChainTransferTaskVo> getInfo(@PathVariable Long taskId) {
        return R.ok(transferService.selectTaskById(taskId));
    }

    /**
     * 创建转账任务(biz_no 幂等,只建单不执行)
     *
     * @param bo 建单参数
     * @return 任务ID
     */
    @SaCheckPermission("chain:transfer:add")
    @Log(title = "链转账任务", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Long> add(@Validated @RequestBody ChainTransferTaskBo bo) {
        return R.ok(transferService.createTask(bo));
    }

    /**
     * 执行转账任务(估费 → 组交易 → 签名 → 广播)
     *
     * @param taskId 任务ID
     * @return 操作结果
     */
    @SaCheckPermission("chain:transfer:execute")
    @Log(title = "链转账任务", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/execute/{taskId}")
    public R<Void> execute(@PathVariable Long taskId) {
        transferService.executeTask(taskId);
        return R.ok();
    }

    /**
     * 重试失败任务(置回待执行,不自动执行)
     *
     * @param taskId 任务ID
     * @return 操作结果
     */
    @SaCheckPermission("chain:transfer:execute")
    @Log(title = "链转账任务", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/retry/{taskId}")
    public R<Void> retry(@PathVariable Long taskId) {
        return toAjax(transferService.retryTask(taskId));
    }

    /**
     * 刷新已广播任务的链上状态
     *
     * @param taskId 任务ID
     * @return 操作结果
     */
    @SaCheckPermission("chain:transfer:query")
    @PutMapping("/refresh/{taskId}")
    public R<Void> refresh(@PathVariable Long taskId) {
        transferService.refreshTask(taskId);
        return R.ok();
    }

}
