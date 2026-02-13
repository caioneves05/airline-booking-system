package com.airline.customer.dto;

import com.airline.customer.domain.Customer;
import com.airline.customer.domain.CustomerStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerResponseDTO {
    private UUID customerId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private LocalDateTime dateOfBirth;
    private String passportNumber;
    private LocalDateTime passportExpiry;
    private String passportCountry;
    private CustomerStatus status;
    private boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CustomerResponseDTO (Customer customer) {
        this.customerId = customer.getId();
        this.email = customer.getEmail();
        this.firstName = customer.getFirstName();
        this.lastName = customer.getLastName();
        this.phoneNumber = customer.getPhoneNumber();
        this.dateOfBirth = customer.getDateOfBirth().atStartOfDay();
        this.passportNumber = customer.getPassportNumber();
        this.passportExpiry = customer.getPassportExpiry().atStartOfDay();
        this.passportCountry = customer.getPassportCountry();
        this.status = customer.getStatus();
        this.emailVerified = customer.isEmailVerified();
        this.createdAt = customer.getCreatedAt();
        this.updatedAt = customer.getUpdatedAt();
    }
}
