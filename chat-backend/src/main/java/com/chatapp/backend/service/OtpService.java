package com.chatapp.backend.service;

import com.chatapp.backend.dto.OtpVerifyRequest;
import com.chatapp.backend.dto.OtpVerifyResponse;
import com.chatapp.backend.dto.UserDto;

public interface OtpService {

    public String sendOtp(String phone);

    public OtpVerifyResponse verifyOtp(OtpVerifyRequest verifyrequest);
    
    public UserDto registerUser(UserDto register);
    
    public boolean hasOtp(String phone);

    public void putOtp(String phone, String otp);

}
