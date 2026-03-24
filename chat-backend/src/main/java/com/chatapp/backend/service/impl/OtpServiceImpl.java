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
import com.chatapp.backend.service.ChatMetrics;

import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * OTP service using Brevo REST API.
 * FLOW
 * ────
 * 1. User provides phone (E.164) + email
 * 2. OTP generated, stored in Redis keyed by phone, sent via Brevo API
 * 3. User enters OTP; verified against Redis; JWT issued
 *
 * Phone = identity key. Email = delivery channel only.
 */
@Service
public class OtpServiceImpl implements OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpServiceImpl.class);

    // E.164 international format: +[country-code][number], e.g. +919876543210
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[1-9]\\d{7,14}$");
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final int OTP_RATE_LIMIT = 5;

    private final StringRedisTemplate redisTemplate;
    private final Random random = new Random();
    private final UserRepository userRepository;
    private final JWTGenerator jwtGenerator;
    private final RoleRepository roleRepository;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final ChatMetrics chatMetrics;

    /** Brevo API Key */
    @Value("${brevo.api-key}")
    private String brevoApiKey;

    /** Email address used as the sender */
    @Value("${brevo.sender-email}")
    private String senderEmail;

    /** App name shown in OTP email subject/body */
    @Value("${app.name:SkibidiChat}")
    private String appName;

    @Autowired
    public OtpServiceImpl(StringRedisTemplate redisTemplate,
                          UserRepository userRepository,
                          JWTGenerator jwtGenerator,
                          RoleRepository roleRepository,
                          AuthenticationManager authenticationManager,
                          CustomUserDetailsService customUserDetailsService,
                          ChatMetrics chatMetrics) {
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.jwtGenerator = jwtGenerator;
        this.roleRepository = roleRepository;
        this.authenticationManager = authenticationManager;
        this.customUserDetailsService = customUserDetailsService;
        this.chatMetrics = chatMetrics;
    }

    /**
     * Sends an OTP to the given email address via Brevo HTTP API.
     *
     * @param phone E.164 phone number (e.g. "+919876543210") — serves as user identifier
     * @param email Email address where OTP is delivered
     */
    public String sendOtp(String phone, String email) {
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            logger.warn("Invalid phone number format: {}", phone);
            throw new IllegalArgumentException("Phone must be in E.164 format, e.g. +919876543210");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Valid email address is required");
        }

        // Rate limit — max 5 OTP requests per phone per hour
        String rateLimitKey = "chat:otp_rate:" + phone;
        String attempts = redisTemplate.opsForValue().get(rateLimitKey);
        int attemptCount = attempts != null ? Integer.parseInt(attempts) : 0;
        if (attemptCount >= OTP_RATE_LIMIT) {
            logger.warn("OTP rate limit exceeded for phone: {}", phone);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many OTP requests. Maximum " + OTP_RATE_LIMIT + " per hour.");
        }

        String otp = String.format("%06d", random.nextInt(900000) + 100000);

        try {
            // Store OTP in Redis keyed by phone (5-minute expiry)
            redisTemplate.opsForValue().set(phone, otp, 5, TimeUnit.MINUTES);
            redisTemplate.opsForValue().increment(rateLimitKey, 1);
            redisTemplate.expire(rateLimitKey, 1, TimeUnit.HOURS);

            // Send OTP via Brevo API
            sendOtpEmail(email, otp);

            logger.info("OTP sent to email={} for phone={} via Brevo", maskEmail(email), phone);
            chatMetrics.incrementOtpRequests();
            return "OTP sent to your email successfully";

        } catch (ResponseStatusException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to send OTP for phone={}: {}", phone, e.getMessage());
            throw new RuntimeException("Failed to send OTP: " + e.getMessage());
        }
    }

    /**
     * Backwards-compatible single-arg override — not used, requires email.
     */
    @Override
    public String sendOtp(String phone) {
        throw new UnsupportedOperationException(
            "Use sendOtp(phone, email) — email is required for OTP delivery");
    }

    private void sendOtpEmail(String toEmail, String otp) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("api-key", brevoApiKey);

        Map<String, Object> sender = new HashMap<>();
        sender.put("name", appName);
        sender.put("email", senderEmail);

        Map<String, String> recipient = new HashMap<>();
        recipient.put("email", toEmail);

        Map<String, Object> body = new HashMap<>();
        body.put("sender", sender);
        body.put("to", Collections.singletonList(recipient));
        body.put("subject", appName + " — Your Verification Code");
        body.put("htmlContent", buildOtpEmailHtml(otp));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity("https://api.brevo.com/v3/smtp/email", request, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("Brevo API error: {}", response.getBody());
                throw new RuntimeException("Email delivery failed to " + maskEmail(toEmail));
            }
        } catch (Exception e) {
            logger.error("Failed to send email via Brevo: {}", e.getMessage());
            throw new RuntimeException("Email delivery failed", e);
        }
    }

    private String buildOtpEmailHtml(String otp) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:24px;
                        border:1px solid #e5e7eb;border-radius:12px;">
              <h2 style="color:#1f2937;margin:0 0 8px">Your verification code</h2>
              <p style="color:#6b7280;margin:0 0 24px">Enter this code to verify your %s account:</p>
              <div style="font-size:36px;font-weight:700;letter-spacing:8px;color:#4F46E5;
                          background:#f5f3ff;padding:16px 24px;border-radius:8px;text-align:center;">
                %s
              </div>
              <p style="color:#9ca3af;margin:24px 0 0;font-size:13px;">
                This code expires in <strong>5 minutes</strong>. If you didn't request this, ignore this email.
              </p>
            </div>
            """.formatted(appName, otp);
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 2) return "***" + email.substring(at);
        return email.charAt(0) + "***" + email.substring(at - 1);
    }

    @Override
    public OtpVerifyResponse verifyOtp(OtpVerifyRequest verifyRequest) {
        chatMetrics.incrementOtpVerifications();
        try {
            if (verifyRequest.getPhone() == null || verifyRequest.getOtp() == null ||
                !PHONE_PATTERN.matcher(verifyRequest.getPhone()).matches() ||
                !verifyRequest.getOtp().matches("\\d{6}")) {
                logger.warn("Invalid OTP verify request: phone={}", verifyRequest.getPhone());
                return new OtpVerifyResponse(verifyRequest.getPhone(), verifyRequest.getOtp(), false, false, null);
            }

            String attemptKey = "chat:otp_verify:" + verifyRequest.getPhone();
            String attempts = redisTemplate.opsForValue().get(attemptKey);
            int attemptCount = attempts != null ? Integer.parseInt(attempts) : 0;
            if (attemptCount >= MAX_VERIFY_ATTEMPTS) {
                logger.warn("OTP verify limit exceeded for phone: {}", verifyRequest.getPhone());
                return new OtpVerifyResponse(verifyRequest.getPhone(), verifyRequest.getOtp(), false, false, null);
            }

            String storedOtp = redisTemplate.opsForValue().get(verifyRequest.getPhone());
            boolean isValid = storedOtp != null && verifyRequest.getOtp().equals(storedOtp);
            boolean userExists = userRepository.existsByPhone(verifyRequest.getPhone());

            redisTemplate.opsForValue().increment(attemptKey, 1);
            redisTemplate.expire(attemptKey, 1, TimeUnit.HOURS);

            if (!isValid) {
                logger.warn("OTP mismatch for phone: {}", verifyRequest.getPhone());
                return new OtpVerifyResponse(verifyRequest.getPhone(), verifyRequest.getOtp(), userExists, false, null);
            }

            redisTemplate.delete(verifyRequest.getPhone());
            redisTemplate.delete(attemptKey);

            Authentication authentication;
            try {
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(verifyRequest.getPhone());
                authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (AuthenticationException ex) {
                logger.error("Auth failed for phone {}: {}", verifyRequest.getPhone(), ex.getMessage());
                return new OtpVerifyResponse(verifyRequest.getPhone(), verifyRequest.getOtp(), userExists, false, null);
            }

            String token = jwtGenerator.generateToken(authentication);
            return new OtpVerifyResponse(verifyRequest.getPhone(), verifyRequest.getOtp(), userExists, true, token);

        } catch (RedisConnectionFailureException ex) {
            logger.error("Redis connection failed during OTP verify: {}", ex.getMessage());
            return new OtpVerifyResponse(verifyRequest.getPhone(), verifyRequest.getOtp(), false, false, null);
        } catch (Exception ex) {
            logger.error("Unexpected error during OTP verify for phone={}: {}", verifyRequest.getPhone(), ex.getMessage());
            return new OtpVerifyResponse(verifyRequest.getPhone(), verifyRequest.getOtp(), false, false, null);
        }
    }

    @Override
    public UserDto registerUser(UserDto register) {
        if (register.getPhone() == null || !PHONE_PATTERN.matcher(register.getPhone()).matches()) {
            throw new IllegalArgumentException("Phone must be in E.164 format, e.g. +919876543210");
        }
        if (userRepository.existsByPhone(register.getPhone())) {
            throw new IllegalStateException("User already exists");
        }
        User user = UserMapper.mapToEntity(register);
        Role role = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Role USER not found"));
        user.setRoles(Collections.singletonList(role));
        User savedUser = userRepository.save(user);
        logger.info("User registered: {}", savedUser.getPhone());
        return UserMapper.mapToDto(savedUser);
    }

    @Override
    public boolean hasOtp(String phone) {
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) return false;
        return redisTemplate.hasKey(phone);
    }

    /** For test purposes — injects OTP directly into Redis without sending email */
    public void putOtp(String phone, String otp) {
        if (phone == null || otp == null ||
            !PHONE_PATTERN.matcher(phone).matches() || !otp.matches("\\d{6}")) {
            throw new IllegalArgumentException("Invalid phone or OTP format");
        }
        redisTemplate.opsForValue().set(phone, otp, 5, TimeUnit.MINUTES);
    }
}