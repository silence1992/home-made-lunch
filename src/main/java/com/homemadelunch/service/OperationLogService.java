package com.homemadelunch.service;

import com.homemadelunch.entity.OperationLog;
import com.homemadelunch.entity.User;
import com.homemadelunch.mapper.OperationLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志服务
 */
@Service
public class OperationLogService {

    @Autowired
    private OperationLogMapper operationLogMapper;

    /**
     * 记录操作日志
     */
    public void log(User user, String module, String action, String content) {
        log(user, module, action, null, content);
    }

    /**
     * 记录操作日志（带日期）
     */
    public void log(User user, String module, String action, LocalDate targetDate, String content) {
        OperationLog log = new OperationLog();
        if (user != null) {
            log.setFamilyId(user.getFamilyId());
            log.setUserId(user.getId());
            log.setUserName(user.getNickname());
        }
        log.setModule(module);
        log.setAction(action);
        log.setTargetDate(targetDate);
        log.setContent(content);
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    /**
     * 查询某天的操作记录
     */
    public List<OperationLog> getLogsByDate(Long familyId, LocalDate date) {
        return operationLogMapper.selectList(
            new LambdaQueryWrapper<OperationLog>()
                .eq(OperationLog::getFamilyId, familyId)
                .eq(OperationLog::getTargetDate, date)
                .orderByDesc(OperationLog::getCreatedAt)
        );
    }

    /**
     * 查询家庭操作记录
     */
    public List<OperationLog> getLogsByFamily(Long familyId, int limit) {
        return operationLogMapper.selectList(
            new LambdaQueryWrapper<OperationLog>()
                .eq(OperationLog::getFamilyId, familyId)
                .orderByDesc(OperationLog::getCreatedAt)
                .last("LIMIT " + limit)
        );
    }
}
