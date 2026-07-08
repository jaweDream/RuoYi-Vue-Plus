package org.dromara.chain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.dromara.chain.api.enums.ProtocolType;
import org.dromara.chain.api.enums.TxStatus;
import org.dromara.chain.api.model.BroadcastResult;
import org.dromara.chain.api.model.FeeEstimate;
import org.dromara.chain.api.model.PreparedTx;
import org.dromara.chain.api.model.SignedTx;
import org.dromara.chain.api.model.TransferRequest;
import org.dromara.chain.api.model.WatchedAsset;
import org.dromara.chain.api.spi.ChainReader;
import org.dromara.chain.api.spi.ChainSigner;
import org.dromara.chain.api.spi.ChainWriter;
import org.dromara.chain.domain.ChainTransferTask;
import org.dromara.chain.domain.bo.ChainTransferTaskBo;
import org.dromara.chain.domain.vo.ChainTransferTaskVo;
import org.dromara.chain.enums.ChainTransferStatus;
import org.dromara.chain.mapper.ChainTransferTaskMapper;
import org.dromara.chain.registry.ChainAdapterRegistry;
import org.dromara.chain.service.IChainAssetService;
import org.dromara.chain.service.IChainTransferService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 链转账任务 服务层实现
 *
 * <p>状态机:待执行 →(条件抢占)执行中 → 已广播 → 成功/失败;失败可重试回待执行。
 * 执行路径刻意不包事务:链上广播不可回滚,以任务状态落库为准绳,而非数据库事务。
 *
 * <p>注意:广播环节的通信类失败存在"实际已上链"的不确定性,重试前应人工核对
 * 出账地址的链上 nonce/交易;按地址串行化与 nonce 台账是后续硬化方向。
 *
 * @author jarvey
 */
@Slf4j
@Service
public class ChainTransferServiceImpl implements IChainTransferService {

    private final ChainAdapterRegistry registry;
    private final ChainTransferTaskMapper taskMapper;
    private final IChainAssetService assetService;

    /**
     * 按协议注册的签名器(未注册的协议在签名环节明确失败)
     */
    private final Map<ProtocolType, ChainSigner> signers = new EnumMap<>(ProtocolType.class);

    public ChainTransferServiceImpl(ChainAdapterRegistry registry, ChainTransferTaskMapper taskMapper,
                                    IChainAssetService assetService, List<ChainSigner> signerList) {
        this.registry = registry;
        this.taskMapper = taskMapper;
        this.assetService = assetService;
        for (ChainSigner signer : signerList) {
            signers.put(signer.protocol(), signer);
        }
    }

    @Override
    public PageResult<ChainTransferTaskVo> selectPageTaskList(ChainTransferTaskBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ChainTransferTask> lqw = Wrappers.lambdaQuery(ChainTransferTask.class)
            .eq(StringUtils.isNotBlank(bo.getBizNo()), ChainTransferTask::getBizNo, bo.getBizNo())
            .eq(StringUtils.isNotBlank(bo.getNetwork()), ChainTransferTask::getNetwork, bo.getNetwork())
            .eq(bo.getAssetId() != null, ChainTransferTask::getAssetId, bo.getAssetId())
            .eq(StringUtils.isNotBlank(bo.getStatus()), ChainTransferTask::getStatus, bo.getStatus())
            .like(StringUtils.isNotBlank(bo.getToAddress()), ChainTransferTask::getToAddress, bo.getToAddress())
            .orderByDesc(ChainTransferTask::getTaskId);
        Page<ChainTransferTaskVo> page = taskMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public ChainTransferTaskVo selectTaskById(Long taskId) {
        return taskMapper.selectVoById(taskId);
    }

    @Override
    public Long createTask(ChainTransferTaskBo bo) {
        // 网络必须已配置且启用;资产必须存在、启用且属于该网络
        registry.getNetworkProps(bo.getNetwork());
        WatchedAsset asset = assetService.getWatched(bo.getAssetId());
        if (!bo.getNetwork().equals(asset.getChainKey().getNetwork())) {
            throw new ServiceException("资产不属于网络 " + bo.getNetwork());
        }
        if (bo.getRawAmount().stripTrailingZeros().scale() > 0) {
            throw new ServiceException("金额必须为最小单位整数(按资产精度换算后传入)");
        }
        if (taskMapper.exists(Wrappers.lambdaQuery(ChainTransferTask.class)
            .eq(ChainTransferTask::getBizNo, bo.getBizNo()))) {
            throw new ServiceException("业务单号已存在: " + bo.getBizNo());
        }
        ChainTransferTask task = MapstructUtils.convert(bo, ChainTransferTask.class);
        task.setTaskId(null);
        task.setProtocol(asset.getChainKey().getProtocol().name());
        task.setStatus(ChainTransferStatus.WAITING.getCode());
        task.setAttemptCount(0);
        taskMapper.insert(task);
        return task.getTaskId();
    }

    @Override
    public void executeTask(Long taskId) {
        // 条件抢占:待执行 → 执行中,天然防并发重复出账
        int seized = taskMapper.update(null, Wrappers.<ChainTransferTask>lambdaUpdate()
            .set(ChainTransferTask::getStatus, ChainTransferStatus.EXECUTING.getCode())
            .eq(ChainTransferTask::getTaskId, taskId)
            .eq(ChainTransferTask::getStatus, ChainTransferStatus.WAITING.getCode()));
        if (seized == 0) {
            throw new ServiceException("任务不在待执行状态,无法执行: " + taskId);
        }
        ChainTransferTask task = taskMapper.selectById(taskId);
        try {
            ChainWriter writer = registry.getWriter(task.getNetwork());
            WatchedAsset asset = assetService.getWatched(task.getAssetId());
            TransferRequest request = TransferRequest.builder()
                .chainKey(asset.getChainKey())
                .asset(asset)
                .fromAddress(task.getFromAddress())
                .toAddress(task.getToAddress())
                .rawAmount(task.getRawAmount().toBigIntegerExact())
                .memo(task.getMemo())
                .build();
            FeeEstimate fee = writer.estimateFee(request);
            PreparedTx prepared = writer.buildTransfer(request);
            ChainSigner signer = signers.get(asset.getChainKey().getProtocol());
            if (signer == null) {
                throw new ServiceException("未注册 " + asset.getChainKey().getProtocol()
                    + " 协议签名器(需按私钥托管方案实现 ChainSigner 并注册为 Bean)");
            }
            SignedTx signed = signer.sign(prepared, task.getFromAddress());
            BroadcastResult result = writer.broadcast(signed);
            if (!result.isAccepted()) {
                throw new ServiceException("节点拒绝广播: " + result.getMessage());
            }
            taskMapper.update(null, Wrappers.<ChainTransferTask>lambdaUpdate()
                .set(ChainTransferTask::getStatus, ChainTransferStatus.BROADCASTED.getCode())
                .set(ChainTransferTask::getTxHash, result.getTxHash())
                .set(ChainTransferTask::getRawFee, new BigDecimal(fee.getRawFee()))
                .set(ChainTransferTask::getLastError, "")
                .set(ChainTransferTask::getAttemptCount, task.getAttemptCount() + 1)
                .eq(ChainTransferTask::getTaskId, taskId));
            log.info("[chain-transfer] 任务 {} 已广播 tx={}", taskId, result.getTxHash());
        } catch (Exception e) {
            log.error("[chain-transfer] 任务 {} 执行失败", taskId, e);
            markFailed(task, e.getMessage());
        }
    }

    @Override
    public int retryTask(Long taskId) {
        int rows = taskMapper.update(null, Wrappers.<ChainTransferTask>lambdaUpdate()
            .set(ChainTransferTask::getStatus, ChainTransferStatus.WAITING.getCode())
            .eq(ChainTransferTask::getTaskId, taskId)
            .eq(ChainTransferTask::getStatus, ChainTransferStatus.FAILED.getCode()));
        if (rows == 0) {
            throw new ServiceException("任务不在失败状态,无法重试: " + taskId);
        }
        return rows;
    }

    @Override
    public void refreshTask(Long taskId) {
        ChainTransferTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在: " + taskId);
        }
        if (!ChainTransferStatus.BROADCASTED.getCode().equals(task.getStatus())) {
            throw new ServiceException("任务不在已广播状态,无需刷新: " + taskId);
        }
        doRefresh(task);
    }

    @Override
    public void refreshBroadcasted() {
        List<ChainTransferTask> tasks = taskMapper.selectList(Wrappers.lambdaQuery(ChainTransferTask.class)
            .eq(ChainTransferTask::getStatus, ChainTransferStatus.BROADCASTED.getCode()));
        for (ChainTransferTask task : tasks) {
            try {
                doRefresh(task);
            } catch (Exception e) {
                log.error("[chain-transfer] 任务 {} 状态刷新失败", task.getTaskId(), e);
            }
        }
    }

    /**
     * 查询链上状态并推进任务状态机
     */
    private void doRefresh(ChainTransferTask task) {
        ChainReader reader = registry.getReader(task.getNetwork());
        TxStatus status = reader.getTxStatus(task.getTxHash());
        switch (status) {
            case SUCCESS -> taskMapper.update(null, Wrappers.<ChainTransferTask>lambdaUpdate()
                .set(ChainTransferTask::getStatus, ChainTransferStatus.SUCCESS.getCode())
                .set(ChainTransferTask::getLastError, "")
                .eq(ChainTransferTask::getTaskId, task.getTaskId())
                .eq(ChainTransferTask::getStatus, ChainTransferStatus.BROADCASTED.getCode()));
            case FAILED -> taskMapper.update(null, Wrappers.<ChainTransferTask>lambdaUpdate()
                .set(ChainTransferTask::getStatus, ChainTransferStatus.FAILED.getCode())
                .set(ChainTransferTask::getLastError, "链上执行失败(revert),手续费已消耗")
                .eq(ChainTransferTask::getTaskId, task.getTaskId())
                .eq(ChainTransferTask::getStatus, ChainTransferStatus.BROADCASTED.getCode()));
            // 交易查不到不自动判败:可能未广播成功或已被替换,留在已广播状态并记录,人工核对
            case NOT_FOUND -> taskMapper.update(null, Wrappers.<ChainTransferTask>lambdaUpdate()
                .set(ChainTransferTask::getLastError, "节点未找到交易,请人工核对是否需要重发")
                .eq(ChainTransferTask::getTaskId, task.getTaskId()));
            case PENDING -> {
                // 未达终态,等待下轮刷新
            }
        }
    }

    /**
     * 置任务失败并累计执行次数
     */
    private void markFailed(ChainTransferTask task, String reason) {
        taskMapper.update(null, Wrappers.<ChainTransferTask>lambdaUpdate()
            .set(ChainTransferTask::getStatus, ChainTransferStatus.FAILED.getCode())
            .set(ChainTransferTask::getLastError, StringUtils.substring(reason, 0, 500))
            .set(ChainTransferTask::getAttemptCount, task.getAttemptCount() + 1)
            .eq(ChainTransferTask::getTaskId, task.getTaskId()));
    }

}
