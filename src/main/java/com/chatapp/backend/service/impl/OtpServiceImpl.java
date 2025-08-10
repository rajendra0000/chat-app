package com.chatapp.backend.service.impl;

import com.chatapp.backend.dto.OtpVerifyRequest;
import com.chatapp.backend.dto.OtpVerifyResponse;
import com.chatapp.backend.dto.UserDto;
import com.chatapp.backend.mapper.UserMapper;
import com.chatapp.backend.model.Role;
import com.chatapp.backend.model.User;
import com.chatapp.backend.repository.RoleRepository;
import com.chatapp.backend.repository.UserRepository;
import com.chatapp.backend.security.CustomUserDetailsService;
import com.chatapp.backend.security.JWTGenerator;
import com.chatapp.backend.service.OtpService;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class OtpServiceImpl implements OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpServiceImpl.class);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{10}$"); // Adjust for your phone format
    private static final int MAX_ATTEMPTS = 5; // Max OTP verification attempts
    private final StringRedisTemplate redisTemplate;
    private final Random random = new Random();
    private final UserRepository userRepository;
    private final JWTGenerator jwtGenerator;
    private final RoleRepository roleRepository;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;

    @Value("${twilio.phone.number}")
    private String twilioPhoneNumber;

    @Autowired
    public OtpServiceImpl(StringRedisTemplate redisTemplate, UserRepository userRepository,
                          JWTGenerator jwtGenerator, RoleRepository roleRepository,
                          AuthenticationManager authenticationManager , CustomUserDetailsService customUserDetailsService) {
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.jwtGenerator = jwtGenerator;
        this.roleRepository = roleRepository;
        this.authenticationManager = authenticationManager;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    public String sendOtp(String phone) {
        // Validate phone number
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            logger.warn("Invalid phone number: {}", phone);
            throw new IllegalArgumentException("Invalid phone number format");
        }

        // Check rate limit
        String attemptKey = "otp_attempts:" + phone;
        String attempts = redisTemplate.opsForValue().get(attemptKey);
        int attemptCount = attempts != null ? Integer.parseInt(attempts) : 0;
        if (attemptCount >= MAX_ATTEMPTS) {
            logger.warn("OTP request limit exceeded for phone: {}", phone);
            throw new IllegalStateException("Too many OTP requests. Try again later.");
        }

        // Generate OTP
        String otp = String.format("%06d", random.nextInt(900000) + 100000);
        System.out.println("HERE IS THE OTP :" + otp);
        try {
            // Store OTP in Redis with 5-minute expiry
            redisTemplate.opsForValue().set(phone, otp, 5, TimeUnit.MINUTES);
            redisTemplate.opsForValue().increment(attemptKey, 1);
            redisTemplate.expire(attemptKey, 1, TimeUnit.HOURS); // Reset attempts after 1 hour

            // Send OTP via Twilio SMS
            Message.creator(
                    new PhoneNumber("+91" + phone),
                    new PhoneNumber(twilioPhoneNumber),
                    "Your OTP is: " + otp
            ).create();

            logger.info("OTP sent successfully to phone: {}", phone);
            return "OTP sent successfully"; // Don't return OTP
        } catch (Exception e) {
            logger.error("Failed to send OTP to phone {}: {}", phone, e.getMessage());
            throw new RuntimeException("Failed to send OTP");
        }
    }

    @Override
    public OtpVerifyResponse verifyOtp(OtpVerifyRequest verifyRequest) {
        try {
            // Validate input
            if (verifyRequest.getPhone() == null || verifyRequest.getOtp() == null ||
                !PHONE_PATTERN.matcher(verifyRequest.getPhone()).matches() ||
                !verifyRequest.getOtp().matches("\\d{6}")) {
                logger.warn("Invalid OTP verification request: phone={} otp={}", verifyRequest.getPhone(), verifyRequest.getOtp());
                return new OtpVerifyResponse(verifyRequest.getPhone(), verifyRequest.getOtp(), false, false, null);
            }

            // Check verification attempts
            String attemptKey = "otp_verify_attempts:" + verifyRequest.getPhone();
            String attempts = redisTemplate.opsForValue().get(attemptKey);
            int attemptCount = attempts != null ? Integer.parseInt(attempts) : 0;
            if (attemptCount >= MAX_ATTEMPTS) {
                logger.warn("OTP verification limit exceeded for phone: {}", verifyRequest.getPhone());
                return new OtpVerifyResponse(verifyRequest.getPhone(), verifyRequest.getOtp(), false, false, null);
            }

            // Retrieve OTP from Redis
            String storedOtp = redisTemplate.opsForValue().get(verifyRequest.getPhone());
            boolean isValid = storedOtp != null && verifyRequest.getOtp().equals(storedOtp);
            boolean userExists = userRepository.existsByPhone(verifyRequest.getPhone());
            logger.debug("OTP valid: {}, User exists: {}", isValid, userExists);

            // Increment verification attempts
            redisTemplate.opsForValue().increment(attemptKey, 1);
            redisTemplate.expire(attemptKey, 1, TimeUnit.HOURS);

            if (!isValid) {
                logger.warn("OTP verification failed for phone: {}", verifyRequest.getPhone());
                return new OtpVerifyResponse(verifyRequest.getPhone(), verifyRequest.getOtp(), userExists, false, null);
            }

            // Clean up OTP and attempts from Redis
            redisTemplate.delete(verifyRequest.getPhone());
            redisTemplate.delete(attemptKey);
            logger.debug("Deleted OTP and attempts for phone: {}", verifyRequest.getPhone());

            // Authenticate user
            Authentication authentication;
            try {
               UserDetails userDetails = customUserDetailsService.loadUserByUsername(verifyRequest.getPhone());
                authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("Authenticated user: {}", verifyRequest.getPhone());
            } catch (AuthenticationException ex) {
                logger.error("Authentication failed for phone {}: {}", verifyRequest.getPhone(), ex.getMessage());
                return new OtpVerifyResponse(verifyRequest.getPhone(), verifyRequest.getOtp(), userExists, false, null);
            }

            // Generate JWT
            String token = jwtGenerator.generateToken(authentication);
            logger.debug("Generated JWT for phone: {}", verifyRequest.getPhone());

            return new OtpVerifyResponse(verifyRequest.getPhone(), verifyRequest.getOtp(), userExists, true, token);
        } catch (RedisConnectionFailureException ex) {
            logger.error("Redis connection failed during OTP verification: {}", ex.getMessage());
            return new OtpVerifyResponse(verifyRequest.getPhone(), verifyRequest.getOtp(), false, false, null);
        } catch (Exception ex) {
            logger.error("Unexpected error during OTP verification for phone {}: {}", verifyRequest.getPhone(), ex.getMessage());
            return new OtpVerifyResponse(verifyRequest.getPhone(), verifyRequest.getOtp(), false, false, null);
        }
    }

    @Override
    public UserDto registerUser(UserDto register) {
        if (register.getPhone() == null || !PHONE_PATTERN.matcher(register.getPhone()).matches()) {
            logger.warn("Invalid phone number during registration: {}", register.getPhone());
            throw new IllegalArgumentException("Invalid phone number format");
        }

        if (userRepository.existsByPhone(register.getPhone())) {
            logger.warn("User already exists with phone: {}", register.getPhone());
            throw new IllegalStateException("User already exists");
        }

        User user = UserMapper.mapToEntity(register);
        Role role = roleRepository.findByName("USER")
                .orElseThrow(() -> {
                    logger.error("Role USER not found");
                    return new IllegalStateException("Role USER not found");
                });
        user.setRoles(Collections.singletonList(role));
        User savedUser = userRepository.save(user);
        logger.info("User registered successfully: {}", savedUser.getPhone());
        return UserMapper.mapToDto(savedUser);
    }

    @Override
    public boolean hasOtp(String phone) {
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            logger.warn("Invalid phone number for hasOtp check: {}", phone);
            return false;
        }
        return redisTemplate.hasKey(phone);
    }

    // For test purposes
    public void putOtp(String phone, String otp) {
        if (phone == null || otp == null || !PHONE_PATTERN.matcher(phone).matches() || !otp.matches("\\d{6}")) {
            logger.warn("Invalid input for putOtp: phone={} otp={}", phone, otp);
            throw new IllegalArgumentException("Invalid phone or OTP format");
        }
        redisTemplate.opsForValue().set(phone, otp, 5, TimeUnit.MINUTES);
        logger.debug("Stored test OTP for phone: {}", phone);
    }
}