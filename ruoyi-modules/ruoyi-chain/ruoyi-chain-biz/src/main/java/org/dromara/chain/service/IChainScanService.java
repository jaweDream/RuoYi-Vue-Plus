package org.dromara.chain.service;

import org.dromara.chain.domain.bo.ChainCursorResetBo;
import org.dromara.chain.domain.bo.ChainDepositRecordBo;
import org.dromara.chain.domain.vo.ChainDepositRecordVo;
import org.dromara.chain.domain.vo.ChainScanCursorVo;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.List;

/**
 * 链扫块 服务层(游标推进、充值入账、游标运维)
 *
 * @author jarvey
 */
public interface IChainScanService {

    /**
     * 扫描全部已启用网络(单个网络失败不影响其他网络)
     */
    void scanAll();

    /**
     * 扫描单个网络:从游标推进至安全高度(链头 - 确认深度),
     * 命中充值地址的转账事件幂等入库
     *
     * @param network 网络名
     */
    void scanNetwork(String network);

    /**
     * 查询全部网络的扫块游标
     *
     * @return 游标列表
     */
    List<ChainScanCursorVo> listCursors();

    /**
     * 重置游标高度(回扫/跳块运维操作)
     *
     * @param bo 重置参数
     * @return 影响行数
     */
    int resetCursor(ChainCursorResetBo bo);

    /**
     * 暂停指定网络扫块
     *
     * @param network 网络名
     * @return 影响行数
     */
    int pauseCursor(String network);

    /**
     * 恢复指定网络扫块
     *
     * @param network 网络名
     * @return 影响行数
     */
    int resumeCursor(String network);

    /**
     * 分页查询充值记录
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 充值记录分页结果
     */
    PageResult<ChainDepositRecordVo> selectPageDepositList(ChainDepositRecordBo bo, PageQuery pageQuery);

}
