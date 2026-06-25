package com.gamesphere.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTeamRequest {

    @NotBlank(message = "Team name is required")
    @Size(min = 3, max = 50, message = "Team name must be between 3 and 50 characters")
    private String name;

    @NotBlank(message = "Team tag is required")
    @Size(min = 2, max = 8, message = "Team tag must be between 2 and 8 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Team tag must contain only uppercase letters and numbers")
    private String tag;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}
