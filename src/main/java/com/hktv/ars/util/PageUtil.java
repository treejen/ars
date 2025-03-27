package com.hktv.ars.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@UtilityClass
public class PageUtil {

    public static Pageable createPageable(Integer pageSize, Integer pageNo, String sort) {
        if (StringUtils.isNotBlank(sort)){
            String sortProperties = sort.split(":")[0];
            Sort.Direction sortDirection = Sort.Direction.valueOf(sort.split(":")[1].toUpperCase(Locale.ROOT));
            return PageRequest.of(pageNo - 1, pageSize, sortDirection, sortProperties);
        }
        return createPageable(pageSize, pageNo);
    }

    public static Pageable createPageable(Integer pageSize, Integer pageNo) {
        return PageRequest.of(pageNo - 1, pageSize);
    }

    public static Pageable createPageable(Integer pageSize, Integer pageNo, List<String> sort) {
        List<Sort.Order> orders = new ArrayList<>();

        for (String s:sort){
            String sortProperties = s.split(":")[0];
            Sort.Direction sortDirection = Sort.Direction.valueOf(s.split(":")[1].toUpperCase(Locale.ROOT));
            Sort.Order order = new Sort.Order(sortDirection, sortProperties);
            orders.add(order);
        }

        return PageRequest.of(pageNo - 1, pageSize, Sort.by(orders));
    }

}
