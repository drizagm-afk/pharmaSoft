package com.edu.upeu.PharmaBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponseDTO {
    public HttpStatus status;
    public String title;
    public String message;
}
