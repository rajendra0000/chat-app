package com.chatapp.backend;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

import com.chatapp.backend.dto.OtpVerifyRequest;
import com.chatapp.backend.dto.OtpVerifyResponse;
import com.chatapp.backend.model.Role;
import com.chatapp.backend.model.User;
import com.chatapp.backend.repository.RoleRepository;
import com.chatapp.backend.repository.UserRepository;
import com.chatapp.backend.security.CustomUserDetailsService;
import com.chatapp.backend.security.JWTGenerator;
import com.chatapp.backend.service.ChatMetrics;
import com.chatapp.backend.service.impl.OtpServiceImpl;

/**
 * Integration-style unit tests for OTP verification flow.
 * Tests: successful verify, wrong OTP, rate limit exceeded, expired OTP.
 */
@ExtendWith(MockitoExtension.class)
class OtpVerificationFlowTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private UserRepository userRepository;
    @Mock private JWTGenerator jwtGenerator;
    @Mock private RoleRepository roleRepository;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private CustomUserDetailsService customUserDetailsService;
    @Mock private ChatMetrics chatMetrics;

    @InjectMocks
    private OtpServiceImpl otpService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("verifyOtp: valid OTP returns success with JWT token")
    void verifyOtp_validOtp_returnsSuccess() {
        String phone = "1234567890";
        String otp = "123456";

        when(valueOperations.get("chat:otp_verify:" + phone)).thenReturn("0");
        when(valueOperations.get(phone)).thenReturn(otp);
        when(userRepository.existsByPhone(phone)).thenReturn(true);
        when(redisTemplate.delete(phone)).thenReturn(true);
        when(redisTemplate.delete("chat:otp_verify:" + phone)).thenReturn(true);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(phone)
                .password("dummy")
                .authorities("ROLE_USER")
                .build();
        when(customUserDetailsService.loadUserByUsername(phone)).thenReturn(userDetails);
        when(jwtGenerator.generateToken(any(Authentication.class))).thenReturn("test-jwt-token");

        OtpVerifyRequest request = new OtpVerifyRequest(phone, otp);
        OtpVerifyResponse response = otpService.verifyOtp(request);

        assertTrue(response.isSuccess());
        assertTrue(response.isUserExists());
        assertEquals("test-jwt-token", response.getToken());
        verify(chatMetrics).incrementOtpVerifications();
    }

    @Test
    @DisplayName("verifyOtp: wrong OTP returns failure")
    void verifyOtp_wrongOtp_returnsFalse() {
        String phone = "1234567890";

        when(valueOperations.get("chat:otp_verify:" + phone)).thenReturn("0");
        when(valueOperations.get(phone)).thenReturn("999999"); // Stored OTP differs
        when(userRepository.existsByPhone(phone)).thenReturn(true);

        OtpVerifyRequest request = new OtpVerifyRequest(phone, "123456");
        OtpVerifyResponse response = otpService.verifyOtp(request);

        assertFalse(response.isSuccess());
        verify(chatMetrics).incrementOtpVerifications();
    }

    @Test
    @DisplayName("verifyOtp: expired OTP (not in Redis) returns failure")
    void verifyOtp_expiredOtp_returnsFalse() {
        String phone = "1234567890";

        when(valueOperations.get("chat:otp_verify:" + phone)).thenReturn("0");
        when(valueOperations.get(phone)).thenReturn(null); // OTP expired
        when(userRepository.existsByPhone(phone)).thenReturn(true);

        OtpVerifyRequest request = new OtpVerifyRequest(phone, "123456");
        OtpVerifyResponse response = otpService.verifyOtp(request);

        assertFalse(response.isSuccess());
    }

    @Test
    @DisplayName("verifyOtp: exceeds verification attempts returns failure")
    void verifyOtp_tooManyAttempts_returnsFalse() {
        String phone = "1234567890";

        when(valueOperations.get("chat:otp_verify:" + phone)).thenReturn("5"); // At limit

        OtpVerifyRequest request = new OtpVerifyRequest(phone, "123456");
        OtpVerifyResponse response = otpService.verifyOtp(request);

        assertFalse(response.isSuccess());
        // Should not even check the OTP value
        verify(valueOperations, never()).get(phone);
    }

    @Test
    @DisplayName("verifyOtp: invalid phone format returns failure")
    void verifyOtp_invalidPhone_returnsFalse() {
        OtpVerifyRequest request = new OtpVerifyRequest("abc", "123456");
        OtpVerifyResponse response = otpService.verifyOtp(request);

        assertFalse(response.isSuccess());
    }

    @Test
    @DisplayName("sendOtp: rate limit exceeded throws 429")
    void sendOtp_rateLimitExceeded_throws429() {
        String phone = "1234567890";
        when(valueOperations.get("chat:otp_rate:" + phone)).thenReturn("5"); // At limit

        assertThrows(ResponseStatusException.class, () -> otpService.sendOtp(phone));
    }
}
