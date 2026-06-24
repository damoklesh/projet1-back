package com.openclassrooms.etudiant.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponseDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String login;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}
