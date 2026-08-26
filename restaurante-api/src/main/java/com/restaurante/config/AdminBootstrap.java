package com.restaurante.config;

import com.restaurante.application.auth.PasswordPolicy;
import com.restaurante.application.common.EmailNormalizer;
import com.restaurante.domain.model.Role;
import com.restaurante.domain.model.RoleName;
import com.restaurante.domain.model.UserAccount;
import com.restaurante.domain.repository.RoleRepository;
import com.restaurante.domain.repository.UserAccountRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap {

    private final BootstrapProperties properties;
    private final RoleRepository roles;
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrap(BootstrapProperties properties, RoleRepository roles,
                          UserAccountRepository users, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.roles = roles;
        this.users = users;
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
            users.save(new UserAccount(
                    email,
                    passwordEncoder.encode(properties.getInitialPassword()),
                    adminRole
            ));
            return;
        }
        if (admin.getRole().getName() != RoleName.ADMIN) {
            admin.setRole(adminRole);
        }
        admin.enable();
        users.save(admin);
    }
}
