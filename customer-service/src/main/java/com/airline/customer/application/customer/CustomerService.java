package com.airline.customer.application.customer;

import ch.qos.logback.classic.spi.LogbackServiceProvider;
import com.airline.customer.domain.Customer;
import com.airline.customer.dto.CustomerResponseDTO;
import com.airline.customer.exceptions.AlreadyExistsException;
import com.airline.customer.infraestructure.dtos.CustomerDto;
import com.airline.customer.repository.ICustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomerService {

    private final ICustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    Logger logger
            = LoggerFactory.getLogger(LogbackServiceProvider.class);

    public CustomerService(ICustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public CustomerResponseDTO createCustomer (CustomerDto customerDTO) throws Exception {
        try {

            Optional<Customer> existingCustomer = customerRepository.findByEmail(customerDTO.getEmail());

            System.out.println(existingCustomer);
            if (existingCustomer.isPresent()) {
                throw new AlreadyExistsException("Customer already exists.");
            }

            Customer customer = new Customer();
            customer.setFirstName(customerDTO.getFirstName());
            customer.setLastName(customerDTO.getLastName());
            customer.setEmail(customerDTO.getEmail());
            customer.setPassword(passwordEncoder.encode(customerDTO.getPassword()));
            customer.setPhoneNumber(customerDTO.getPhoneNumber());

            Customer savedCustomer = customerRepository.save(customer);

            CustomerResponseDTO responseDTO = new CustomerResponseDTO();
            responseDTO.setCustomerId(savedCustomer.getId());
            responseDTO.setFirstName(savedCustomer.getFirstName());
            responseDTO.setLastName(savedCustomer.getLastName());
            responseDTO.setEmail(savedCustomer.getEmail());
            responseDTO.setPhoneNumber(savedCustomer.getPhoneNumber());

            return responseDTO;
        } catch (Exception error) {
            logger.error("Error to create customer: {}", error.getMessage());
            throw error;
        }
    }
}
