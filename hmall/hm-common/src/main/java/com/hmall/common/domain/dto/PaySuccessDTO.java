package com.hmall.common.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaySuccessDTO {
    private Long orderId;
    private Integer status;
    private LocalDateTime payTime;
}
