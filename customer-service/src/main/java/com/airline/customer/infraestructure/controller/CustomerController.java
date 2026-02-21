package com.airline.customer.infraestructure.controller;

import com.airline.customer.application.customer.CustomerService;
import com.airline.customer.infraestructure.dto.customer.CustomerResponseDTO;
import com.airline.customer.infraestructure.dto.customer.CustomerRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService createCustomerUseCase;

    public CustomerController(CustomerService createCustomerUseCase) {
        this.createCustomerUseCase = createCustomerUseCase;
    }

    @PostMapping("/create")
    public ResponseEntity<CustomerResponseDTO> createCustomer(@RequestBody @Valid CustomerRequestDTO customerDTO) throws Exception {
        return ResponseEntity.ok(createCustomerUseCase.createCustomer(customerDTO));
    }
}

