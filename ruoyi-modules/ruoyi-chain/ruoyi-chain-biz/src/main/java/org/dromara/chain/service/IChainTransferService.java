package org.dromara.chain.service;

import org.dromara.chain.domain.bo.ChainTransferTaskBo;
import org.dromara.chain.domain.vo.ChainTransferTaskVo;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;

/**
 * 链转账任务 服务层(建单 → 执行 → 状态跟踪的状态机编排)
 *
 * @author jarvey
 */
public interface IChainTransferService {

    /**
     * 分页查询转账任务
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 任务分页结果
     */
    PageResult<ChainTransferTaskVo> selectPageTaskList(ChainTransferTaskBo bo, PageQuery pageQuery);

    /**
     * 查询任务详情
     *
     * @param taskId 任务ID
     * @return 任务详情
     */
    ChainTransferTaskVo selectTaskById(Long taskId);

    /**
     * 创建转账任务(biz_no 幂等,重复建单抛业务异常;只建单不执行)
     *
     * @param bo 建单参数
     * @return 任务ID
     */
    Long createTask(ChainTransferTaskBo bo);

    /**
     * 执行转账任务:条件抢占(待执行→执行中)后 估费 → 组交易 → 签名 → 广播;
     * 成功置为已广播,任一环节失败置为失败并记录原因
     *
     * @param taskId 任务ID
     */
    void executeTask(Long taskId);

    /**
     * 重试失败任务(失败→待执行,不自动执行)
     *
     * @param taskId 任务ID
     * @return 影响行数
     */
    int retryTask(Long taskId);

    /**
     * 刷新单个已广播任务的链上状态
     *
     * @param taskId 任务ID
     */
    void refreshTask(Long taskId);

    /**
     * 批量刷新全部已广播任务的链上状态(供定时任务调用)
     */
    void refreshBroadcasted();

}
