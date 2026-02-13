package com.airline.customer.repository;

import com.airline.customer.domain.Customer;
import com.airline.customer.domain.CustomerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ICustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByEmailAndStatus(String email, CustomerStatus status);

}
