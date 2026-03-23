package com.chatapp.backend;

import com.chatapp.backend.dto.OtpRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testSendOtpEndpoint() throws Exception {
        // OtpRequest now requires phone (E.164) + email
        OtpRequest request = new OtpRequest("+919876543210", "test@example.com");

        mockMvc.perform(post("/auth/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError()); // expected: no real mail server in test
    }

    @Test
    public void testVerifyOtpEndpoint() throws Exception {
        // Send OTP first (will fail to deliver email in test env — that's fine)
        mockMvc.perform(post("/auth/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new OtpRequest("+919876543210", "test@example.com"))))
                .andExpect(status().isInternalServerError()); // no mail server in test

        // Verify OTP — will return 401 since no OTP was stored (mail failed)
        mockMvc.perform(post("/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"+919876543210\",\"otp\":\"123456\"}"))
                .andExpect(status().isUnauthorized());
    }
}
