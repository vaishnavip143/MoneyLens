package com.moneylens.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorizedTransaction {

    private String description;
    private String category;
    private Double confidence;
    private Boolean isRecurring;
    private String merchant;
}
