package org.dromara.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.workflow.common.constant.FlowConstant;
import org.dromara.workflow.domain.bo.ProcessInstanceBo;
import org.dromara.workflow.domain.vo.ProcessInstanceVo;
import org.dromara.workflow.flowable.CustomDefaultProcessDiagramGenerator;
import org.dromara.workflow.service.IActProcessInstanceService;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 流程实例 服务层实现
 *
 * @author may
 */

@RequiredArgsConstructor
@Service
public class ActProcessInstanceServiceImpl implements IActProcessInstanceService {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;

    @Value("${flowable.activity-font-name}")
    private String activityFontName;

    @Value("${flowable.label-font-name}")
    private String labelFontName;

    @Value("${flowable.annotation-font-name}")
    private String annotationFontName;

    /**
     * 分页查询正在运行的流程实例
     *
     * @param processInstanceBo 参数
     */
    @Override
    public TableDataInfo<ProcessInstanceVo> getProcessInstanceRunningByPage(ProcessInstanceBo processInstanceBo) {
        List<ProcessInstanceVo> list = new ArrayList<>();
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
        query.processInstanceTenantId(TenantHelper.getTenantId());
        if (StringUtils.isNotBlank(processInstanceBo.getName())) {
            query.processInstanceNameLikeIgnoreCase("%" + processInstanceBo.getName() + "%");
        }
        if (StringUtils.isNotBlank(processInstanceBo.getStartUserId())) {
            query.startedBy(processInstanceBo.getStartUserId());
        }
        if (StringUtils.isNotBlank(processInstanceBo.getBusinessKey())) {
            query.processInstanceBusinessKey(processInstanceBo.getBusinessKey());
        }
        List<ProcessInstance> processInstances = query.listPage(processInstanceBo.getPageNum(), processInstanceBo.getPageSize());
        for (ProcessInstance processInstance : processInstances) {
            list.add(BeanUtil.toBean(processInstance, ProcessInstanceVo.class));
        }
        long count = query.count();
        return new TableDataInfo<>(list, count);
    }

    /**
     * 分页查询已结束的流程实例
     *
     * @param processInstanceBo 参数
     */
    @Override
    public TableDataInfo<ProcessInstanceVo> getProcessInstanceFinishByPage(ProcessInstanceBo processInstanceBo) {
        List<ProcessInstanceVo> list = new ArrayList<>();
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().finished()
            .orderByProcessInstanceEndTime().desc();
        query.processInstanceTenantId(TenantHelper.getTenantId());
        if (StringUtils.isNotEmpty(processInstanceBo.getName())) {
            query.processInstanceNameLikeIgnoreCase("%" + processInstanceBo.getName() + "%");
        }
        if (StringUtils.isNotEmpty(processInstanceBo.getStartUserId())) {
            query.startedBy(processInstanceBo.getStartUserId());
        }
        if (StringUtils.isNotBlank(processInstanceBo.getBusinessKey())) {
            query.processInstanceBusinessKey(processInstanceBo.getBusinessKey());
        }
        List<HistoricProcessInstance> historicProcessInstances = query.listPage(processInstanceBo.getPageNum(), processInstanceBo.getPageSize());
        for (HistoricProcessInstance historicProcessInstance : historicProcessInstances) {
            list.add(BeanUtil.toBean(historicProcessInstance, ProcessInstanceVo.class));
        }
        long count = query.count();
        return new TableDataInfo<>(list, count);
    }

    /**
     * 通过流程实例id获取历史流程图
     *
     * @param processInstanceId 流程实例id
     * @param response          响应
     */
    @Override
    public void getHistoryProcessImage(String processInstanceId, HttpServletResponse response) {
        // 设置页面不缓存
        response.setHeader("Pragma", "no-cache");
        response.addHeader("Cache-Control", "must-revalidate");
        response.addHeader("Cache-Control", "no-cache");
        response.addHeader("Cache-Control", "no-store");
        response.setDateHeader("Expires", 0);
        InputStream inputStream = null;
        try {
            String processDefinitionId;
            // 获取当前的流程实例
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            // 如果流程已经结束，则得到结束节点
            if (Objects.isNull(processInstance)) {
                HistoricProcessInstance pi = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
                processDefinitionId = pi.getProcessDefinitionId();
            } else {
                // 根据流程实例ID获得当前处于活动状态的ActivityId合集
                ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
                processDefinitionId = pi.getProcessDefinitionId();
            }

            // 获得活动的节点
            List<HistoricActivityInstance> highLightedFlowList = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).orderByHistoricActivityInstanceStartTime().asc().list();

            List<String> highLightedFlows = new ArrayList<>();
            List<String> highLightedNodes = new ArrayList<>();
            //高亮
            for (HistoricActivityInstance tempActivity : highLightedFlowList) {
                if (FlowConstant.SEQUENCE_FLOW.equals(tempActivity.getActivityType())) {
                    //高亮线
                    highLightedFlows.add(tempActivity.getActivityId());
                } else {
                    //高亮节点
                    if (tempActivity.getEndTime() == null) {
                        highLightedNodes.add(Color.RED.toString() + tempActivity.getActivityId());
                    } else {
                        highLightedNodes.add(tempActivity.getActivityId());
                    }
                }
            }
            List<String> highLightedNodeList = new ArrayList<>();
            //运行中的节点
            List<String> redNodeCollect = StreamUtils.filter(highLightedNodes, e -> e.contains(Color.RED.toString()));
            //排除与运行中相同的节点
            for (String nodeId : highLightedNodes) {
                if (!nodeId.contains(Color.RED.toString()) && !redNodeCollect.contains(Color.RED + nodeId)) {
                    highLightedNodeList.add(nodeId);
                }
            }
            highLightedNodeList.addAll(redNodeCollect);
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
            CustomDefaultProcessDiagramGenerator diagramGenerator = new CustomDefaultProcessDiagramGenerator();
            inputStream = diagramGenerator.generateDiagram(bpmnModel, "png", highLightedNodeList, highLightedFlows, activityFontName, labelFontName, annotationFontName, null, 1.0, true);
            // 响应相关图片
            response.setContentType("image/png");

            byte[] bytes = IOUtils.toByteArray(inputStream);
            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write(bytes);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}