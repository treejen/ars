package com.hktv.ars.service;


import com.hktv.ars.data.SystemVariableData;
import com.hktv.ars.data.base.PaginationResultData;
import com.hktv.ars.model.SystemVariable;

import java.util.List;
import java.util.Map;

public interface SystemVariableService {

    boolean updateSystemVariable(SystemVariableData requestData);

    PaginationResultData<SystemVariableData> getAllSystemVariables(Integer pageSize, Integer pageNo, String sort);

    SystemVariable findByName(String name);

    List<SystemVariable> findSystemVariablesByNames(List<String> names);

    Map<String, SystemVariable> findSystemVariablesByNameMap(List<String> names);

    Integer getInt(String name, Integer defaultValue);

}
