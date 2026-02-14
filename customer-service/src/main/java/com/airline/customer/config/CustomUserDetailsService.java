package com.airline.customer.config;

import com.airline.customer.repository.ICustomerRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final ICustomerRepository customerRepository;

    public CustomUserDetailsService(ICustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return (UserDetails) customerRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Email not found"));
    }
}
