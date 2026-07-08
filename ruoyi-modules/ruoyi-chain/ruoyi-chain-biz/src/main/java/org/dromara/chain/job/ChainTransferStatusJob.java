package org.dromara.chain.job;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.model.dto.ExecuteResult;
import lombok.RequiredArgsConstructor;
import org.dromara.chain.service.IChainTransferService;
import org.springframework.stereotype.Component;

/**
 * 链转账状态刷新定时任务(轮询已广播任务的链上确认结果)
 *
 * @author jarvey
 */
@Component
@RequiredArgsConstructor
@JobExecutor(name = "chainTransferStatusJob")
public class ChainTransferStatusJob {

    private final IChainTransferService transferService;

    /**
     * 刷新全部已广播任务状态
     *
     * @param jobArgs 任务执行参数
     * @return 执行结果
     */
    public ExecuteResult jobExecute(JobArgs jobArgs) {
        transferService.refreshBroadcasted();
        return ExecuteResult.success("chain transfer status refreshed");
    }

}
