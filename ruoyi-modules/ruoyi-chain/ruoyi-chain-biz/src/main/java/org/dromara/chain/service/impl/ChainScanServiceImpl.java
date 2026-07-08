package org.dromara.chain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.chain.api.model.AssetTransferEvent;
import org.dromara.chain.api.model.ChainKey;
import org.dromara.chain.api.model.WatchedAsset;
import org.dromara.chain.api.spi.ChainReader;
import org.dromara.chain.config.ChainProperties;
import org.dromara.chain.domain.ChainDepositRecord;
import org.dromara.chain.domain.ChainScanCursor;
import org.dromara.chain.domain.bo.ChainCursorResetBo;
import org.dromara.chain.domain.bo.ChainDepositRecordBo;
import org.dromara.chain.domain.vo.ChainDepositRecordVo;
import org.dromara.chain.domain.vo.ChainScanCursorVo;
import org.dromara.chain.enums.ChainDepositStatus;
import org.dromara.chain.mapper.ChainDepositRecordMapper;
import org.dromara.chain.mapper.ChainScanCursorMapper;
import org.dromara.chain.registry.ChainAdapterRegistry;
import org.dromara.chain.service.IChainAddressService;
import org.dromara.chain.service.IChainAssetService;
import org.dromara.chain.service.IChainScanService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * 链扫块 服务层实现
 *
 * <p>编排职责(链无关,写一次):游标推进、确认深度预留、充值地址归属匹配、幂等入库;
 * "某高度上有哪些转账"这一链相关问题全部交给 {@link ChainReader}。
 *
 * <p>安全原则:任一高度解析失败都不推进游标(推进即漏账),靠下轮任务重试;
 * 重扫/并发的重复入账由 (network, tx_hash, event_index) 唯一键兜底。
 *
 * @author jarvey
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ChainScanServiceImpl implements IChainScanService {

    /**
     * 游标状态-运行
     */
    private static final String CURSOR_RUNNING = "0";

    /**
     * 游标状态-暂停
     */
    private static final String CURSOR_PAUSED = "1";

    private final ChainAdapterRegistry registry;
    private final ChainProperties properties;
    private final ChainScanCursorMapper cursorMapper;
    private final ChainDepositRecordMapper depositMapper;
    private final IChainAssetService assetService;
    private final IChainAddressService addressService;

    @Override
    public void scanAll() {
        if (!properties.isEnabled()) {
            log.debug("[chain-scan] 模块未启用(chain.enabled=false),跳过扫块");
            return;
        }
        for (ChainKey key : registry.enabledNetworks()) {
            try {
                scanNetwork(key.getNetwork());
            } catch (Exception e) {
                log.error("[chain-scan] 网络扫描失败: {}", key.canonical(), e);
            }
        }
    }

    @Override
    public void scanNetwork(String network) {
        ChainProperties.NetworkProps props = registry.getNetworkProps(network);
        ChainReader reader = registry.getReader(network);
        ChainScanCursor cursor = getOrInitCursor(network, props, reader);
        if (CURSOR_PAUSED.equals(cursor.getStatus())) {
            log.debug("[chain-scan] {} 游标已暂停,跳过", network);
            return;
        }
        List<WatchedAsset> assets = assetService.listWatched(network);
        if (assets.isEmpty()) {
            log.debug("[chain-scan] {} 无启用资产,跳过", network);
            return;
        }
        // 未配置充值地址时不推进游标:配置补齐后可从当前位置续扫,不漏历史充值
        Map<String, Long> depositAddresses = addressService.mapDepositAddresses(network);
        if (depositAddresses.isEmpty()) {
            log.debug("[chain-scan] {} 无启用充值地址,暂不推进游标", network);
            return;
        }

        int confirmations = props.getConfirmations() != null
            ? props.getConfirmations() : reader.finality().getRecommendedConfirmations();
        long safeHeight = reader.latestHeight() - confirmations;
        long from = cursor.getScannedHeight() + 1;
        long to = Math.min(safeHeight, from + props.getScanBatchSize() - 1L);
        for (long height = from; height <= to; height++) {
            List<AssetTransferEvent> events;
            try {
                events = reader.extractTransfers(height, assets);
            } catch (Exception e) {
                // 解析失败不推进游标(推进即漏账),记录原因等待下轮重试
                cursor.setLastError(StringUtils.substring(e.getMessage(), 0, 500));
                cursorMapper.updateById(cursor);
                log.error("[chain-scan] {} 高度 {} 解析失败,游标停在 {}", network, height, cursor.getScannedHeight(), e);
                return;
            }
            for (AssetTransferEvent event : events) {
                String toAddress = event.getToAddress();
                Long addressId = toAddress == null ? null : depositAddresses.get(toAddress.toLowerCase());
                if (addressId != null) {
                    saveDeposit(event, addressId);
                }
            }
            cursor.setScannedHeight(height);
            cursor.setLastError("");
            cursorMapper.updateById(cursor);
        }
    }

    @Override
    public List<ChainScanCursorVo> listCursors() {
        return cursorMapper.selectVoList(Wrappers.lambdaQuery(ChainScanCursor.class)
            .orderByAsc(ChainScanCursor::getNetwork));
    }

    @Override
    public int resetCursor(ChainCursorResetBo bo) {
        int rows = cursorMapper.update(null, Wrappers.<ChainScanCursor>lambdaUpdate()
            .set(ChainScanCursor::getScannedHeight, bo.getScannedHeight())
            .set(ChainScanCursor::getLastError, "")
            .eq(ChainScanCursor::getNetwork, bo.getNetwork()));
        if (rows == 0) {
            throw new ServiceException("游标不存在(网络尚未扫描过): " + bo.getNetwork());
        }
        return rows;
    }

    @Override
    public int pauseCursor(String network) {
        return updateCursorStatus(network, CURSOR_PAUSED);
    }

    @Override
    public int resumeCursor(String network) {
        return updateCursorStatus(network, CURSOR_RUNNING);
    }

    @Override
    public PageResult<ChainDepositRecordVo> selectPageDepositList(ChainDepositRecordBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ChainDepositRecord> lqw = Wrappers.lambdaQuery(ChainDepositRecord.class)
            .eq(StringUtils.isNotBlank(bo.getNetwork()), ChainDepositRecord::getNetwork, bo.getNetwork())
            .eq(bo.getAssetId() != null, ChainDepositRecord::getAssetId, bo.getAssetId())
            .eq(StringUtils.isNotBlank(bo.getTxHash()), ChainDepositRecord::getTxHash, bo.getTxHash())
            .eq(StringUtils.isNotBlank(bo.getToAddress()), ChainDepositRecord::getToAddress, bo.getToAddress())
            .eq(StringUtils.isNotBlank(bo.getStatus()), ChainDepositRecord::getStatus, bo.getStatus())
            .orderByDesc(ChainDepositRecord::getDepositId);
        Page<ChainDepositRecordVo> page = depositMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 取网络游标,首次扫描时按配置起点(缺省为当前链头,不回扫历史)初始化
     */
    private ChainScanCursor getOrInitCursor(String network, ChainProperties.NetworkProps props, ChainReader reader) {
        ChainScanCursor cursor = cursorMapper.selectOne(Wrappers.lambdaQuery(ChainScanCursor.class)
            .eq(ChainScanCursor::getNetwork, network));
        if (cursor != null) {
            return cursor;
        }
        long scanned = props.getStartHeight() != null ? props.getStartHeight() - 1 : reader.latestHeight();
        cursor = new ChainScanCursor();
        cursor.setProtocol(props.getProtocol().name());
        cursor.setNetwork(network);
        cursor.setScannedHeight(Math.max(scanned, 0));
        cursor.setStatus(CURSOR_RUNNING);
        cursor.setLastError("");
        cursorMapper.insert(cursor);
        log.info("[chain-scan] {} 初始化游标,起始已扫高度 {}", network, cursor.getScannedHeight());
        return cursor;
    }

    /**
     * 充值事件幂等入库(先查后插 + 唯一键兜底)
     */
    private void saveDeposit(AssetTransferEvent event, Long addressId) {
        boolean exists = depositMapper.exists(Wrappers.lambdaQuery(ChainDepositRecord.class)
            .eq(ChainDepositRecord::getNetwork, event.getChainKey().getNetwork())
            .eq(ChainDepositRecord::getTxHash, event.getTxHash())
            .eq(ChainDepositRecord::getEventIndex, event.getEventIndex()));
        if (exists) {
            return;
        }
        ChainDepositRecord record = new ChainDepositRecord();
        record.setProtocol(event.getChainKey().getProtocol().name());
        record.setNetwork(event.getChainKey().getNetwork());
        record.setAssetId(event.getAssetId());
        record.setAddressId(addressId);
        record.setTxHash(event.getTxHash());
        record.setEventIndex(event.getEventIndex());
        record.setHeight(event.getHeight());
        record.setBlockHash(event.getBlockHash());
        record.setBlockTime(event.getBlockTime() == null
            ? null : LocalDateTime.ofInstant(event.getBlockTime(), ZoneId.systemDefault()));
        record.setFromAddress(event.getFromAddress());
        record.setToAddress(event.getToAddress());
        record.setRawAmount(new BigDecimal(event.getRawAmount()));
        record.setStatus(ChainDepositStatus.CONFIRMED.getCode());
        try {
            depositMapper.insert(record);
            log.info("[chain-scan] {} 入账 {} raw={} tx={}",
                record.getNetwork(), record.getToAddress(), record.getRawAmount(), record.getTxHash());
        } catch (DuplicateKeyException e) {
            // 并发/重扫命中唯一键,幂等忽略
            log.debug("[chain-scan] 重复充值事件已忽略: {}#{}", event.getTxHash(), event.getEventIndex());
        }
    }

    /**
     * 更新游标运行状态
     */
    private int updateCursorStatus(String network, String status) {
        int rows = cursorMapper.update(null, Wrappers.<ChainScanCursor>lambdaUpdate()
            .set(ChainScanCursor::getStatus, status)
            .eq(ChainScanCursor::getNetwork, network));
        if (rows == 0) {
            throw new ServiceException("游标不存在(网络尚未扫描过): " + network);
        }
        return rows;
    }

}
