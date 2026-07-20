package com.moneylens.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

    private Long id;
    private String name;
    private String email;
    private Double monthlyBudget;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateUserRequest {
        private String name;
        private String email;
        private Double monthlyBudget;
    }
}
