package com.restaurante.application.employee;

import com.restaurante.application.common.EmailNormalizer;
import com.restaurante.domain.model.RestaurantUserProfile;
import com.restaurante.domain.model.Role;
import com.restaurante.domain.model.RoleName;
import com.restaurante.domain.model.UserAccount;
import com.restaurante.domain.repository.RestaurantUserProfileRepository;
import com.restaurante.domain.repository.RoleRepository;
import com.restaurante.domain.repository.UserAccountRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.security.JwtData;
import com.restaurante.web.dto.employee.CreateEmployeeRequest;
import com.restaurante.web.dto.employee.EmployeeResponse;
import com.restaurante.web.dto.employee.OperationalRoleResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.restaurante.web.dto.employee.UpdateEmployeeRequest;

import java.util.List;

@Service
public class EmployeeService {

    private final UserAccountRepository users;
    private final RestaurantUserProfileRepository profiles;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(UserAccountRepository users,
                           RestaurantUserProfileRepository profiles,
                           RoleRepository roles,
                           PasswordEncoder passwordEncoder) {
        this.users = users;
        this.profiles = profiles;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public EmployeeResponse createEmployee(
            CreateEmployeeRequest request,
            Authentication authentication) {

        Long restaurantId = getRestaurantId(authentication);

        String email = EmailNormalizer.normalize(request.email());
        String employeeCode = request.codigoEmpleado().trim();

        if (users.findByEmail(email).isPresent()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "employee_login_in_use",
                    "Identificador de acceso en uso",
                    "El identificador de acceso ya está en uso"
            );
        }

        if (profiles.existsByRestaurantIdAndEmployeeCode(
                restaurantId,
                employeeCode)) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "employee_code_in_use",
                    "Código de empleado en uso",
                    "El código de empleado ya está en uso"
            );
        }

        if (!isOperationalRole(request.rol())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_operational_role",
                    "Rol operativo inválido",
                    "Debe seleccionarse un rol operativo válido"
            );
        }

        Role role = roles.findByName(request.rol())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT,
                        "role_not_configured",
                        "Rol no configurado",
                        "El rol seleccionado no se encuentra configurado"
                ));

        UserAccount account = users.save(
                new UserAccount(
                        email,
                        passwordEncoder.encode(request.password()),
                        role
                )
        );

        RestaurantUserProfile profile = profiles.save(
                new RestaurantUserProfile(
                        account.getId(),
                        restaurantId,
                        employeeCode,
                        request.nombres().trim(),
                        request.apellidos().trim(),
                        request.fechaContratacion()
                )
        );

        return new EmployeeResponse(
                account.getId(),
                profile.getEmployeeCode(),
                profile.getFirstName(),
                profile.getLastName(),
                account.getEmail(),
                profile.getHireDate(),
                account.getRole().getName(),
                account.isEnabled()
        );
    }

    @Transactional(readOnly = true)
    public List<OperationalRoleResponse> getOperationalRoles() {
        return List.of(
                new OperationalRoleResponse(RoleName.WAITER, "Mesero"),
                new OperationalRoleResponse(RoleName.KITCHEN, "Cocina"),
                new OperationalRoleResponse(RoleName.CASHIER, "Cajero")
        );
    }

    private boolean isOperationalRole(RoleName role) {
        return role == RoleName.WAITER
                || role == RoleName.KITCHEN
                || role == RoleName.CASHIER;
    }

    private Long getRestaurantId(Authentication authentication) {
        if (authentication == null
                || !(authentication.getDetails() instanceof JwtData jwtData)) {

            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "invalid_authenticated_user",
                    "Usuario no autenticado",
                    "No fue posible identificar al usuario autenticado"
            );
        }

        RestaurantUserProfile profile = profiles
                .findById(jwtData.userId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT,
                        "restaurant_profile_not_found",
                        "Perfil de restaurante no encontrado",
                        "El usuario autenticado no tiene un perfil asociado al restaurante"
                ));

        return profile.getRestaurantId();
    }

    @Transactional
    public EmployeeResponse updateEmployee(
            Long employeeId,
            UpdateEmployeeRequest request,
            Authentication authentication) {

        Long restaurantId = getRestaurantId(authentication);

        RestaurantUserProfile profile = profiles
                .findById(employeeId)
                .filter(existing -> existing.getRestaurantId().equals(restaurantId))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "employee_not_found",
                        "Empleado no encontrado",
                        "El empleado seleccionado no existe"
                ));

        UserAccount account = users.findById(employeeId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "employee_account_not_found",
                        "Cuenta de empleado no encontrada",
                        "La cuenta asociada al empleado no existe"
                ));

        String email = EmailNormalizer.normalize(request.email());
        String employeeCode = request.codigoEmpleado().trim();

        if (users.findByEmailAndIdNot(email, employeeId).isPresent()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "employee_login_in_use",
                    "Identificador de acceso en uso",
                    "El identificador de acceso ya está en uso"
            );
        }

        if (profiles.existsByRestaurantIdAndEmployeeCodeAndIdNot(
                restaurantId,
                employeeCode,
                employeeId)) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "employee_code_in_use",
                    "Código de empleado en uso",
                    "El código de empleado ya está en uso"
            );
        }

        if (!isOperationalRole(request.rol())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_operational_role",
                    "Rol operativo inválido",
                    "Debe seleccionarse un rol operativo válido"
            );
        }

        Role role = roles.findByName(request.rol())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT,
                        "role_not_configured",
                        "Rol no configurado",
                        "El rol seleccionado no se encuentra configurado"
                ));

        boolean accessChanged =
                !account.getEmail().equals(email)
                        || account.getRole().getName() != request.rol();

        account.setEmail(email);
        account.setRole(role);

        if (accessChanged) {
            account.incrementTokenVersion();
        }

        profile.updateInformation(
                employeeCode,
                request.nombres().trim(),
                request.apellidos().trim(),
                request.fechaContratacion()
        );

        users.save(account);
        profiles.save(profile);

        return new EmployeeResponse(
                account.getId(),
                profile.getEmployeeCode(),
                profile.getFirstName(),
                profile.getLastName(),
                account.getEmail(),
                profile.getHireDate(),
                account.getRole().getName(),
                account.isEnabled()
        );
    }
}