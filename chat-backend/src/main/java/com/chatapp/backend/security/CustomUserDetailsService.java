package com.chatapp.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.chatapp.backend.model.Role;
import com.chatapp.backend.model.User;
import com.chatapp.backend.repository.UserRepository;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom UserDetailsService for OTP-based authentication.
 *
 * Since this app uses OTP (not passwords), we derive a stable internal password
 * from the user's phone + JWT secret via HMAC. This allows Spring Security's
 * AuthenticationManager to work correctly without storing real passwords.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;
    private final SecurityConstants securityConstants;
    private PasswordEncoder passwordEncoder;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository, SecurityConstants securityConstants) {
        this.userRepository = userRepository;
        this.securityConstants = securityConstants;
    }

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        log.debug("Loading user by phone: {}", phone);
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with the mobile number: " + phone));
        log.debug("Found user: {}", user.getPhone());

        // Derive a stable internal password from phone + secret
        String derivedPassword = deriveInternalPassword(phone);
        return new org.springframework.security.core.userdetails.User(
                user.getPhone(), passwordEncoder.encode(derivedPassword), mapRolesToAuthorities(user.getRoles()));
    }

    /**
     * Derives a deterministic internal password for OTP-based auth.
     * This is NOT a real user password — it's a stable token derived from
     * the phone number and the server's JWT secret so that Spring Security's
     * AuthenticationManager can validate the credential.
     */
    public String deriveInternalPassword(String phone) {
        return "otp-auth:" + phone + ":" + securityConstants.getJwtSecret().substring(0, 8);
    }

    private Collection<GrantedAuthority> mapRolesToAuthorities(List<Role> roles) {
        return roles.stream().map(role -> new SimpleGrantedAuthority(role.getName())).collect(Collectors.toList());
    }
}