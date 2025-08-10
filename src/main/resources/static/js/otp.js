let currentStep = 1;
let countdownTimer;
let timeLeft = 60;
let userPhone = '';
let isExistingUser = false;

// Handle Send OTP
document.getElementById('send-otp-form').addEventListener('submit', function (e) {
    e.preventDefault();
    
    const phone = document.getElementById('mobile').value;
    const sendBtn = document.getElementById('send-btn');
    
    // Validate phone number
    if (!/^\d{10}$/.test(phone)) {
        showResult('Please enter a valid 10-digit phone number', 'error', 'result-step1');
        return;
    }
    
    userPhone = phone;
    sendBtn.classList.add('loading');
    sendBtn.querySelector('span').textContent = 'Sending...';
    
    // Check if user exists and send OTP
    fetch('/auth/check-user', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ phone })
    })
    .then(res => res.json())
    .then(data => {
        if (data.userExists) {
            isExistingUser = true;
            // Send OTP for existing user
            return fetch('/auth/send-otp', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ phone })
            }).then(res => res.text());
        } else {
            isExistingUser = false;
            // Directly go to registration for new user
            showResult('New user, please register', 'success', 'result-step1');
            setTimeout(() => {
                showRegistrationForm();
            }, 1500);
            throw new Error('New user, skipping OTP send');
        }
    })
    .then(data => {
        sendBtn.classList.remove('loading');
        sendBtn.querySelector('span').textContent = 'Send OTP';
        showResult(data, 'success', 'result-step1');
        setTimeout(() => {
            nextStep();
            startCountdown();
        }, 1500);
    })
    .catch(err => {
        sendBtn.classList.remove('loading');
        sendBtn.querySelector('span').textContent = 'Send OTP';
        if (err.message !== 'New user, skipping OTP send') {
            showResult('Error sending OTP', 'error', 'result-step1');
            console.error(err);
        }
    });
});

// Handle Verify OTP
document.getElementById('verify-otp-form').addEventListener('submit', function (e) {
    e.preventDefault();
    
    const phone = userPhone;
    const otp = getOTPValue();
    const verifyBtn = document.getElementById('verify-btn');
    
    if (otp.length !== 6) {
        showResult('Please enter complete 6-digit OTP', 'error', 'result-step2');
        return;
    }
    
    verifyBtn.classList.add('loading');
    verifyBtn.querySelector('span').textContent = 'Verifying...';
    
    // Verify OTP
    fetch('/auth/verify-otp', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ phone, otp })
    })
    .then(res => res.json())
    .then(data => {
        verifyBtn.classList.remove('loading');
        verifyBtn.querySelector('span').textContent = 'Verify OTP';
        
        if (data.success) {
            showResult('✅ OTP verified successfully!', 'success', 'result-step2');
            document.getElementById('step2').classList.add('completed');
            clearInterval(countdownTimer);
            localStorage.setItem('Authorization', data.token);
            
            setTimeout(() => {
                if (isExistingUser) {
                    showWelcomeMessage(data.userData);
                    setTimeout(() => {
                        window.location.href = 'chat.html';
                    }, 2000);
                } else {
                    showResult('OTP verified, redirecting to welcome page', 'success', 'result-step2');
                    setTimeout(() => {
                        showWelcomeMessage(data.userData);
                        setTimeout(() => {
                            window.location.href = 'chat.html';
                        }, 2000);
                    }, 1500);
                }
            }, 1500);
        } else {
            showResult('❌ Invalid OTP. Please try again.', 'error', 'result-step2');
            clearOTPInputs();
        }
    })
    .catch(err => {
        verifyBtn.classList.remove('loading');
        verifyBtn.querySelector('span').textContent = 'Verify OTP';
        showResult('Error verifying OTP', 'error', 'result-step2');
        console.error(err);
    });
});

// Handle Registration
document.getElementById('registration-form').addEventListener('submit', function (e) {
    e.preventDefault();
    
    const formData = {
        phone: userPhone,
        fullName: document.getElementById('fullName').value.trim(),
        email: document.getElementById('email').value.trim(),
        dateOfBirth: document.getElementById('dateOfBirth').value,
        gender: document.getElementById('gender').value,
        address: document.getElementById('address').value.trim()
    };
    
    // Basic validation
    if (!formData.fullName || !formData.email || !formData.dateOfBirth || !formData.gender || !formData.address) {
        showResult('Please fill in all required fields', 'error', 'result-step3');
        return;
    }
    
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(formData.email)) {
        showResult('Please enter a valid email address', 'error', 'result-step3');
        return;
    }
    
    const birthDate = new Date(formData.dateOfBirth);
    const today = new Date();
    const age = today.getFullYear() - birthDate.getFullYear();
    const monthDiff = today.getMonth() - birthDate.getMonth();
    
    if (age < 13 || (age === 13 && monthDiff < 0) || (age === 13 && monthDiff === 0 && today.getDate() < birthDate.getDate())) {
        showResult('You must be at least 13 years old to register', 'error', 'result-step3');
        return;
    }
    
    const registerBtn = document.getElementById('register-btn');
    registerBtn.classList.add('loading');
    registerBtn.querySelector('span').textContent = 'Registering...';
    
    // Register user
    fetch('/auth/register', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(formData)
    })
    .then(res => res.json())
    .then(data => {
        registerBtn.classList.remove('loading');
        registerBtn.querySelector('span').textContent = 'Complete Registration';
        
        if (data.success) {
            showResult('🎉 Registration completed successfully! Please verify OTP.', 'success', 'result-step3');
            document.getElementById('step3').classList.add('completed');
            
            // Send OTP after registration
            fetch('/auth/send-otp', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ phone: userPhone })
            })
            .then(res => res.text())
            .then(() => {
                setTimeout(() => {
                    goBackToOTP();
                    startCountdown();
                }, 1500);
            })
            .catch(err => {
                showResult('Error sending OTP after registration', 'error', 'result-step3');
                console.error(err);
            });
        } else {
            showResult(data.message || 'Registration failed. Please try again.', 'error', 'result-step3');
        }
    })
    .catch(err => {
        registerBtn.classList.remove('loading');
        registerBtn.querySelector('span').textContent = 'Complete Registration';
        showResult('Error during registration', 'error', 'result-step3');
        console.error(err);
    });
});

// Show registration form for new users
function showRegistrationForm() {
    updateHeader('Complete Registration', 'Please fill in your details to complete registration');
    
    document.getElementById('send-otp-step').classList.remove('active');
    document.getElementById('registration-step').classList.add('active');
    document.getElementById('step1').classList.remove('active');
    document.getElementById('step1').classList.add('completed');
    document.getElementById('step3').classList.add('active');
    currentStep = 3;
    
    setTimeout(() => {
        document.getElementById('fullName').focus();
    }, 500);
}

// Show welcome message
function showWelcomeMessage(userData = null) {
    updateHeader('Welcome Back!', 'You have been successfully authenticated');
    
    document.getElementById('verify-otp-step').classList.remove('active');
    document.getElementById('registration-step').classList.remove('active');
    document.getElementById('welcome-step').classList.add('active');
    document.getElementById('step3').classList.add('completed');
    currentStep = 4;
    
    if (userData) {
        document.getElementById('user-phone').textContent = userData.phone || '';
        document.getElementById('user-name').textContent = userData.fullName || 'User';
        document.getElementById('user-email').textContent = userData.email || '';
    }
}

// Update header text
function updateHeader(title, subtitle) {
    document.getElementById('header-title').textContent = title;
    document.getElementById('header-subtitle').textContent = subtitle;
}

// OTP Input handling
const otpInputs = document.querySelectorAll('.otp-digit');

otpInputs.forEach((input, index) => {
    input.addEventListener('input', function(e) {
        e.target.value = e.target.value.replace(/[^0-9]/g, '');
        
        if (e.target.value) {
            if (index < otpInputs.length - 1) {
                otpInputs[index + 1].focus();
            }
        }
    });
    
    input.addEventListener('keydown', function(e) {
        if (e.key === 'Backspace' && !e.target.value && index > 0) {
            otpInputs[index - 1].focus();
        }
        
        if (!/[0-9]/.test(e.key) && !['Backspace', 'Delete', 'Tab', 'ArrowLeft', 'ArrowRight'].includes(e.key)) {
            e.preventDefault();
        }
    });
    
    input.addEventListener('paste', function(e) {
        e.preventDefault();
        const pastedData = e.clipboardData.getData('text');
        const numericData = pastedData.replace(/[^0-9]/g, '');
        
        if (numericData.length <= 6) {
            const digits = numericData.split('');
            digits.forEach((digit, i) => {
                if (otpInputs[i]) {
                    otpInputs[i].value = digit;
                }
            });
            
            const nextEmptyIndex = Math.min(digits.length, otpInputs.length - 1);
            otpInputs[nextEmptyIndex].focus();
        }
    });
});

function getOTPValue() {
    return Array.from(otpInputs).map(input => input.value).join('');
}

function clearOTPInputs() {
    otpInputs.forEach(input => input.value = '');
    otpInputs[0].focus();
}

function nextStep() {
    document.getElementById('send-otp-step').classList.remove('active');
    document.getElementById('verify-otp-step').classList.add('active');
    document.getElementById('step1').classList.remove('active');
    document.getElementById('step1').classList.add('completed');
    document.getElementById('step2').classList.add('active');
    currentStep = 2;
    
    setTimeout(() => {
        otpInputs[0].focus();
    }, 500);
}

function goBack() {
    document.getElementById('verify-otp-step').classList.remove('active');
    document.getElementById('registration-step').classList.remove('active');
    document.getElementById('send-otp-step').classList.add('active');
    document.getElementById('step2').classList.remove('active');
    document.getElementById('step2').classList.add('inactive');
    document.getElementById('step3').classList.remove('active');
    document.getElementById('step3').classList.add('inactive');
    document.getElementById('step1').classList.remove('completed');
    document.getElementById('step1').classList.add('active');
    currentStep = 1;
    clearInterval(countdownTimer);
    hideResult('result-step1');
    hideResult('result-step2');
    hideResult('result-step3');
    
    // Reset header
    updateHeader('Secure Login', 'Verify your identity with OTP authentication');
}

function goBackToOTP() {
    document.getElementById('registration-step').classList.remove('active');
    document.getElementById('verify-otp-step').classList.add('active');
    document.getElementById('step3').classList.remove('active');
    document.getElementById('step3').classList.add('inactive');
    document.getElementById('step2').classList.remove('completed');
    document.getElementById('step2').classList.add('active');
    currentStep = 2;
    
    // Reset header
    updateHeader('Secure Login', 'Verify your identity with OTP authentication');
    
    setTimeout(() => {
        otpInputs[0].focus();
    }, 500);
}

function logout() {
    userPhone = '';
    isExistingUser = false;
    currentStep = 1;
    
    document.getElementById('send-otp-form').reset();
    document.getElementById('verify-otp-form').reset();
    document.getElementById('registration-form').reset();
    
    localStorage.removeItem('Authorization');
    location.reload();
}

function showResult(message, type, resultId) {
    const result = document.getElementById(resultId);
    result.textContent = message;
    result.className = `result show ${type}`;
    
    if (type === 'success') {
        setTimeout(() => {
            hideResult(resultId);
        }, 5000);
    }
}

function hideResult(resultId) {
    const result = document.getElementById(resultId);
    result.classList.remove('show');
}

function startCountdown() {
    timeLeft = 60;
    const countdown = document.getElementById('countdown');
    const timer = document.getElementById('timer');
    
    countdownTimer = setInterval(() => {
        timeLeft--;
        countdown.textContent = timeLeft;
        
        if (timeLeft <= 0) {
            clearInterval(countdownTimer);
            timer.innerHTML = 'Didn\'t receive OTP? <button class="resend-btn" onclick="resendOTP()">Resend OTP</button>';
        }
    }, 1000);
}

function resendOTP() {
    const phone = document.getElementById('mobile').value;
    
    fetch('/auth/send-otp', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ phone })
    })
    .then(res => res.text())
    .then(data => {
        showResult('OTP resent successfully to ' + phone, 'success', 'result-step2');
        startCountdown();
        document.getElementById('timer').innerHTML = 'Resend OTP in <span id="countdown">60</span>s';
    })
    .catch(err => {
        showResult('Error resending OTP', 'error', 'result-step2');
        console.error(err);
    });
}

// Form validation helpers
function validateEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

function validatePhone(phone) {
    const phoneRegex = /^\d{10}$/;
    return phoneRegex.test(phone);
}

function calculateAge(birthDate) {
    const today = new Date();
    const birth = new Date(birthDate);
    let age = today.getFullYear() - birth.getFullYear();
    const monthDiff = today.getMonth() - birth.getMonth();
    
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
        age--;
    }
    
    return age;
}

// Auto-focus on phone input when page loads
window.addEventListener('load', () => {
    document.getElementById('mobile').focus();
});

// Handle keyboard navigation
document.addEventListener('keydown', function(e) {
    if (e.key === 'Enter') {
        e.preventDefault();
        
        if (currentStep === 1) {
            document.getElementById('send-otp-form').dispatchEvent(new Event('submit'));
        } else if (currentStep === 2) {
            document.getElementById('verify-otp-form').dispatchEvent(new Event('submit'));
        } else if (currentStep === 3) {
            document.getElementById('registration-form').dispatchEvent(new Event('submit'));
        }
    }
});

// Real-time validation for form fields
document.getElementById('mobile').addEventListener('input', function(e) {
    e.target.value = e.target.value.replace(/[^0-9]/g, '');
    if (e.target.value.length > 10) {
        e.target.value = e.target.value.slice(0, 10);
    }
});

// Real-time email validation
document.getElementById('email').addEventListener('blur', function(e) {
    const email = e.target.value.trim();
    if (email && !validateEmail(email)) {
        showResult('Please enter a valid email address', 'error', 'result-step3');
        e.target.focus();
    }
});

// Real-time name validation
document.getElementById('fullName').addEventListener('input', function(e) {
    e.target.value = e.target.value.replace(/[^a-zA-Z\s]/g, '');
});

// Disable future dates for date of birth
document.getElementById('dateOfBirth').addEventListener('input', function(e) {
    const selectedDate = new Date(e.target.value);
    const today = new Date();
    
    if (selectedDate > today) {
        showResult('Date of birth cannot be in the future', 'error', 'result-step3');
        e.target.value = '';
    }
});