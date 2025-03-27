package com.hktv.ars.data.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * @author Meow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationResultData<T> {

    private List<T> list;

    private PaginationData pagination;

    private static <T> PaginationData buildPaginationData(Page<T> page) {
        return PaginationData.builder()
                .currentPage(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    public static <T, E> PaginationResultData<T> convertToPaginationData(Page<E> page, List<T> content) {
        return PaginationResultData.<T>builder()
                .list(content)
                .pagination(buildPaginationData(page)).build();
    }

    public static <T, E> PaginationResultData<T> convertToPaginationData(Page<E> page, Function<E, T> func) {
        return PaginationResultData.<T>builder()
                .list(page.getContent().stream().map(func).toList())
                .pagination(buildPaginationData(page)).build();
    }

    public static <T> PaginationResultData<T> convertListToPaginationData(List<T> list, Integer pageSize, Integer pageNumber) {
        int totalPages = (int) Math.ceil((double) list.size() / pageSize);
        return PaginationResultData.<T>builder()
                .list(list)
                .pagination(PaginationData.builder()
                        .currentPage(pageNumber)
                        .pageSize(pageSize)
                        .totalElements(list.size())
                        .totalPages(totalPages)
                        .build())
                .build();
    }
}
