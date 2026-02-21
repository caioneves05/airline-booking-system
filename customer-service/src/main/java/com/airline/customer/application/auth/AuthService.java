package com.airline.customer.application.auth;

import com.airline.customer.application.Jwt.JwtService;
import com.airline.customer.domain.Customer;
import com.airline.customer.infraestructure.dto.auth.AuthRequestDTO;
import com.airline.customer.infraestructure.dto.auth.AuthResponseDTO;
import com.airline.customer.infraestructure.repository.ICustomerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final ICustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            ICustomerRepository customerRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponseDTO login(AuthRequestDTO request) {

        Customer customer = customerRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("Invalid credentials")
                );

        boolean valid = passwordEncoder.matches(
                request.getPassword(),
                customer.getPassword()
        );

        if (!valid) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtService.generateToken(customer);

        return new AuthResponseDTO(token);
    }
}

