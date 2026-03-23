package com.chatapp.backend.ai;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.chatapp.backend.model.Role;
import com.chatapp.backend.model.User;
import com.chatapp.backend.repository.RoleRepository;
import com.chatapp.backend.repository.UserRepository;

/**
 * Auto-creates the Kalori AI system user at startup.
 * Runs before DatabaseSeeder (@Order(1)) so the bot is available for seeding.
 */
@Component
@Order(1)
public class AiUserInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AiUserInitializer.class);

    public static final String KALORI_PHONE = "AI_KALORI";
    public static final String KALORI_NAME = "Kalori AI";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AiConfig aiConfig;

    public AiUserInitializer(UserRepository userRepository, RoleRepository roleRepository, AiConfig aiConfig) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.aiConfig = aiConfig;
    }

    @Override
    public void run(String... args) {
        if (!aiConfig.isEnabled()) {
            log.info("AI feature disabled, skipping Kalori user initialization");
            return;
        }

        if (userRepository.findByPhone(KALORI_PHONE).isPresent()) {
            log.info("Kalori AI user already exists");
            return;
        }

        Role userRole = roleRepository.findByName("USER").orElseGet(() -> {
            Role role = new Role();
            role.setName("USER");
            return roleRepository.save(role);
        });

        User kalori = new User();
        kalori.setPhone(KALORI_PHONE);
        kalori.setName(KALORI_NAME);
        kalori.setStatus("active");
        kalori.setRoles(Collections.singletonList(userRole));
        userRepository.save(kalori);

        log.info("Created Kalori AI system user (phone={})", KALORI_PHONE);
    }
}
