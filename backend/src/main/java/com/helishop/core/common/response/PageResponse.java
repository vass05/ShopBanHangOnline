package com.helishop.core.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cấu trúc phản hồi phân trang chuẩn")
public class PageResponse<T> {

    @Schema(description = "Số trang hiện tại (1-based)", example = "1")
    private int page;

    @Schema(description = "Số lượng phần tử trên mỗi trang", example = "10")
    private int size;

    @Schema(description = "Tổng số phần tử tìm thấy trong toàn bộ DB", example = "120")
    private long totalElements;

    @Schema(description = "Tổng số trang", example = "12")
    private int totalPages;

    @Schema(description = "Danh sách dữ liệu bản ghi của trang hiện tại")
    private List<T> items;

    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .page(page.getNumber() + 1)
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .items(page.getContent())
                .build();
    }
}
