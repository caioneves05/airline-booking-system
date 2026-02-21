package com.airline.customer.infraestructure.controller;

import com.airline.customer.application.auth.AuthService;
import com.airline.customer.infraestructure.dto.auth.AuthRequestDTO;
import com.airline.customer.infraestructure.dto.auth.AuthResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody @Valid AuthRequestDTO authRequest) {
        return ResponseEntity.ok().body(authService.login(authRequest));
    }
}
