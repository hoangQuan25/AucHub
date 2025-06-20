package com.example.notifications.dto; // Adjust package if needed

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page; // Import Spring Data Page

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagedResultDto<T> {

    private List<T> content = Collections.emptyList(); // The list of items on the current page
    private int pageNumber;    // Current page number (0-indexed)
    private int pageSize;      // Number of items requested per page
    private long totalElements; // Total number of items across all pages
    private int totalPages;    // Total number of pages available
    private boolean isLast;    // Is this the last page?
    private boolean isFirst;   // Is this the first page?
    private int numberOfElements; // Number of elements actually returned on this page

    public static <T> PagedResultDto<T> fromPage(Page<T> page) {
        if (page == null) {
            // Or throw an exception, depending on desired behavior
            return new PagedResultDto<>();
        }
        return new PagedResultDto<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                page.isFirst(),
                page.getNumberOfElements()
        );
    }
}