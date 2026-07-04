package com.financeflow.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIReportResponse {

    private String title;
    private String content;
    private Instant generatedAt;
    private boolean fromAi;
}
