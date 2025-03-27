package com.hktv.ars.service.impl;


import com.hktv.ars.data.SystemVariableData;
import com.hktv.ars.data.base.PaginationResultData;
import com.hktv.ars.enums.CustomErrorLogMessage;
import com.hktv.ars.exception.CustomRuntimeException;
import com.hktv.ars.repository.SystemVariableDao;
import com.hktv.ars.service.SsoUserService;
import com.hktv.ars.service.SystemVariableService;
import com.hktv.ars.util.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.hktv.ars.model.SystemVariable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class SystemVariableServiceImpl implements SystemVariableService {

    private final SystemVariableDao systemVariableDao;
    private final SsoUserService ssoUserService;

    @Override
    @Transactional
    public boolean updateSystemVariable(SystemVariableData requestData) {
        SystemVariable systemVariable = systemVariableDao.findByName(requestData.getName()).orElse(null);
        if (systemVariable == null) {
            systemVariable = new SystemVariable();
            systemVariable.setName(requestData.getName());
            systemVariable.setCreatedBy(ssoUserService.getCurrentUserId());
        }

        systemVariable.setValue(requestData.getValue());
        systemVariable.setDescription(requestData.getDescription());
        systemVariable.setLastModifiedBy(ssoUserService.getCurrentUserId());

        systemVariableDao.save(systemVariable);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResultData<SystemVariableData> getAllSystemVariables(Integer pageSize, Integer pageNo, String sort) {
        Pageable pageable = PageUtil.createPageable(pageSize, pageNo, sort);
        Page<SystemVariable> systemVariablePage = systemVariableDao.findSystemVariables(pageable);
        return PaginationResultData.convertToPaginationData(systemVariablePage, this::generateSystemVariableData);

    }

    @Override
    @Transactional(readOnly = true)
    public SystemVariable findByName(String name) {
        return systemVariableDao.findByName(name)
                .orElseThrow(() -> new CustomRuntimeException(CustomErrorLogMessage.SYSTEM_VARIABLE_NOT_FOUND, name));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemVariable> findSystemVariablesByNames(List<String> names) {
        return systemVariableDao.findAllInNameList(names);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, SystemVariable> findSystemVariablesByNameMap(List<String> names) {
        Map<String, SystemVariable> result = new HashMap<>();
        List<SystemVariable> systemVariableList = systemVariableDao.findAllInNameList(names);
        for (SystemVariable systemVariable : systemVariableList) {
            result.put(systemVariable.getName(), systemVariable);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getInt(String name, Integer defaultValue) {
        try {
            Optional<SystemVariable> optionalSystemVariable = systemVariableDao.findByName(name);
            if (optionalSystemVariable.isPresent()) {
                return Integer.parseInt(optionalSystemVariable.get().getValue());
            }
            log.info("System variable name: [{}] not existed, use default value: [{}]", name, defaultValue);
        } catch (Exception e) {
            log.error("System variable get Integer fail, use default value: [{}]. error: {}", defaultValue, e.getMessage());
        }
        return defaultValue;
    }

    private Boolean getBoolean(String name, Boolean defaultValue) {
        try {
            Optional<SystemVariable> optionalSystemVariable = systemVariableDao.findByName(name);
            return optionalSystemVariable
                    .map(systemVariable -> systemVariable.getValue().equals("1"))
                    .orElse(defaultValue);
        } catch (CustomRuntimeException cu) {
            log.error(String.valueOf(cu));
            log.error(cu.getLogMessage());
        }

        return defaultValue;
    }

    private SystemVariableData generateSystemVariableData(SystemVariable systemVariable) {
        return SystemVariableData.builder()
                .name(systemVariable.getName())
                .value(systemVariable.getValue())
                .description(systemVariable.getDescription())
                .build();
    }
}
