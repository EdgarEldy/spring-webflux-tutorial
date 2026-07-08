package edgareldy.springwebfluxtutorial.dto.common;

import java.util.List;

/**
 * Generic paginated content DTO used as the data payload of ApiResponse on every list
 * endpoint, instead of a plain list. Built manually in the service layer via Mono.zip between
 * the paginated Flux (LIMIT/OFFSET) and a total count Mono, since R2DBC repositories have no
 * native Pageable support.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }
}
