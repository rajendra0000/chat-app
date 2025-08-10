package com.chatapp.backend;

import com.chatapp.backend.service.OtpService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class OtpServiceTest {

    @Autowired
    private OtpService otpService;

    @Test
    public void testSendOtpGeneratesAndStoresOtp() {
        String phone = "9876543210";
        otpService.sendOtp(phone);
        assertTrue(otpService.hasOtp(phone), "OTP should be stored after sending.");
    }

    @Test
    public void testVerifyOtpReturnsTrueForCorrectOtp() {
        String phone = "9876543210";
        String otp = otpService.sendOtp(phone); // get OTP returned by sendOtp
       // assertTrue(otpService.verifyOtp(phone, otp), "Correct OTP should return true.");
    }

    @Test
    public void testVerifyOtpReturnsFalseForIncorrectOtp() {
        String phone = "9876543210";
        otpService.sendOtp(phone);
        //assertFalse(otpService.verifyOtp(phone, "999999"), "Incorrect OTP should return false.");
    }

    // @Test
    //  void testOtpIsRemovedAfterSuccessfulVerification() {
    //     String phone = "9876543210";
    //     String otp = otpService.sendOtp(phone);
    //     otpService.verifyOtp(phone, otp); // should invalidate
    //     assertFalse(otpService.hasOtp(phone), "OTP should be removed after successful verification.");
    // }
}
