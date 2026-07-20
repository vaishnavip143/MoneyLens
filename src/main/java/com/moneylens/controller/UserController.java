package com.moneylens.controller;

import com.moneylens.dto.UserDTO;
import com.moneylens.exception.ResourceNotFoundException;
import com.moneylens.model.User;
import com.moneylens.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping
    @Operation(summary = "Create a new user")
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO.CreateUserRequest request) {
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .monthlyBudget(request.getMonthlyBudget())
                .build();

        user = userRepository.save(user);
        return ResponseEntity.ok(UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .monthlyBudget(user.getMonthlyBudget())
                .build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .monthlyBudget(user.getMonthlyBudget())
                .build());
    }

    @PutMapping("/{id}/budget")
    @Operation(summary = "Update monthly budget")
    public ResponseEntity<UserDTO> updateBudget(
            @PathVariable Long id,
            @RequestParam Double budget) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setMonthlyBudget(budget);
        user = userRepository.save(user);
        return ResponseEntity.ok(UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .monthlyBudget(user.getMonthlyBudget())
                .build());
    }
}
