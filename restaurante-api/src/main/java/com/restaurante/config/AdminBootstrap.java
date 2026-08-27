package com.restaurante.config;

import com.restaurante.application.auth.PasswordPolicy;
import com.restaurante.application.common.EmailNormalizer;
import com.restaurante.domain.model.RestaurantUserProfile;
import com.restaurante.domain.model.Role;
import com.restaurante.domain.model.RoleName;
import com.restaurante.domain.model.UserAccount;
import com.restaurante.domain.repository.RestaurantUserProfileRepository;
import com.restaurante.domain.repository.RoleRepository;
import com.restaurante.domain.repository.UserAccountRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;

@Component
public class AdminBootstrap {

    private static final long INITIAL_RESTAURANT_ID = 1L;
    private static final String INITIAL_ADMIN_FIRST_NAME = "Administrador";
    private static final String INITIAL_ADMIN_LAST_NAME = "Inicial";

    private final BootstrapProperties properties;
    private final RoleRepository roles;
    private final UserAccountRepository users;
    private final RestaurantUserProfileRepository profiles;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrap(BootstrapProperties properties, RoleRepository roles,
                          UserAccountRepository users,
                          RestaurantUserProfileRepository profiles,
                          PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.roles = roles;
        this.users = users;
        this.profiles = profiles;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void provisionAdmin() {
        if (!PasswordPolicy.isValid(properties.getInitialPassword())) {
            throw new IllegalStateException("INITIAL_ADMIN_PASSWORD must satisfy the password policy");
        }
        String email = EmailNormalizer.normalize(properties.getEmail());
        Role adminRole = roles.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role is missing from database migration"));
        UserAccount admin = users.findByEmail(email).orElse(null);
        if (admin == null) {
            admin = users.save(new UserAccount(
                    email,
                    passwordEncoder.encode(properties.getInitialPassword()),
                    adminRole
            ));
        } else {
            if (admin.getRole().getName() != RoleName.ADMIN) {
                admin.setRole(adminRole);
            }
            admin.enable();
            admin = users.save(admin);
        }

        provisionOperationalProfile(admin);
    }

    private void provisionOperationalProfile(UserAccount admin) {
        if (profiles.existsById(admin.getId())) {
            return;
        }

        String employeeCode = "ADMIN-" + String.format(
                Locale.ROOT,
                "%04d",
                admin.getId()
        );

        profiles.save(new RestaurantUserProfile(
                admin.getId(),
                INITIAL_RESTAURANT_ID,
                employeeCode,
                INITIAL_ADMIN_FIRST_NAME,
                INITIAL_ADMIN_LAST_NAME,
                LocalDate.now()
        ));
    }
}