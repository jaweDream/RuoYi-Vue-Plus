package org.dromara.chain.job;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.model.dto.ExecuteResult;
import lombok.RequiredArgsConstructor;
import org.dromara.chain.service.IChainScanService;
import org.springframework.stereotype.Component;

/**
 * 链扫块定时任务(建议 SnailJob 配置为固定间隔触发,如 15~30 秒)
 *
 * <p>单次执行按各网络的 scan-batch-size 限幅,不会长时间占用调度线程;
 * 单网络失败不影响其他网络,游标停在失败高度等待下轮重试。
 *
 * @author jarvey
 */
@Component
@RequiredArgsConstructor
@JobExecutor(name = "chainScanJob")
public class ChainScanJob {

    private final IChainScanService scanService;

    /**
     * 扫描全部已启用网络
     *
     * @param jobArgs 任务执行参数
     * @return 执行结果
     */
    public ExecuteResult jobExecute(JobArgs jobArgs) {
        scanService.scanAll();
        return ExecuteResult.success("chain scan finished");
    }

}
