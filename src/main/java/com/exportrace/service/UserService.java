package com.exportrace.service;

import com.exportrace.dto.CreateUserRequest;
import com.exportrace.dto.ResetPasswordRequest;
import com.exportrace.dto.UpdateUserRequest;
import com.exportrace.dto.UserAdminDTO;
import com.exportrace.entity.Role;
import com.exportrace.entity.User;
import com.exportrace.repository.RoleRepository;
import com.exportrace.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditService auditService;

    public List<UserAdminDTO> getAllUsers(String search, String roleFilter, String statusFilter) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String term = "%" + search.toLowerCase().trim() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("nombre")), term);
                Predicate surnameMatch = cb.like(cb.lower(root.get("apellido")), term);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), term);
                Predicate areaMatch = cb.like(cb.lower(root.get("area")), term);
                predicates.add(cb.or(nameMatch, surnameMatch, emailMatch, areaMatch));
            }

            if (roleFilter != null && !roleFilter.isBlank() && !"ALL".equalsIgnoreCase(roleFilter)) {
                predicates.add(cb.equal(root.get("role").get("nombre"), roleFilter.toUpperCase()));
            }

            if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
                boolean active = "ACTIVO".equalsIgnoreCase(statusFilter);
                predicates.add(cb.equal(root.get("activo"), active));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return userRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(UserAdminDTO::new)
                .collect(Collectors.toList());
    }

    public UserAdminDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        return new UserAdminDTO(user);
    }

    @Transactional
    public UserAdminDTO createUser(CreateUserRequest req, String adminEmail, HttpServletRequest request) {
        if (userRepository.existsByEmail(req.getEmail().trim().toLowerCase())) {
            throw new RuntimeException("El correo ya se encuentra registrado: " + req.getEmail());
        }

        Role role = roleRepository.findByNombre(req.getRol().toUpperCase())
                .orElseThrow(() -> new RuntimeException("Rol no válido o inexistente: " + req.getRol()));

        if (Boolean.FALSE.equals(role.getActivo())) {
            throw new RuntimeException("No se puede asignar un rol inactivo: " + role.getNombre());
        }

        User user = new User();
        user.setNombre(req.getNombre().trim());
        user.setApellido(req.getApellido() != null ? req.getApellido().trim() : null);
        user.setEmail(req.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setArea(req.getArea() != null ? req.getArea().trim() : "Operaciones");
        user.setRole(role);
        user.setActivo(true);
        user.setFechaCreacion(LocalDateTime.now());

        User saved = userRepository.save(user);

        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        Long adminId = admin != null ? admin.getId() : null;
        String adminName = admin != null ? admin.getNombre() : "ADMIN";
        String adminRole = admin != null && admin.getRole() != null ? admin.getRole().getNombre() : "ADMINISTRADOR";

        auditService.logAction(
                adminId,
                adminName,
                adminRole,
                "USER_CREATED",
                "USUARIOS",
                "User",
                String.valueOf(saved.getId()),
                "Creación de nuevo usuario: " + saved.getEmail() + " con rol " + role.getNombre(),
                null,
                "Email: " + saved.getEmail() + ", Rol: " + role.getNombre() + ", Área: " + saved.getArea(),
                "EXITOSO",
                request
        );

        return new UserAdminDTO(saved);
    }

    @Transactional
    public UserAdminDTO updateUser(Long id, UpdateUserRequest req, String adminEmail, HttpServletRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        String prevRole = user.getRole() != null ? user.getRole().getNombre() : "";
        String prevStatus = user.getEstado();
        String prevSnapshot = "Nombre: " + user.getNombre() + ", Rol: " + prevRole + ", Estado: " + prevStatus;

        if (req.getRol() != null && !req.getRol().isBlank() && !req.getRol().equalsIgnoreCase(prevRole)) {
            // Check if user is last active admin
            if ("ADMINISTRADOR".equalsIgnoreCase(prevRole) && Boolean.TRUE.equals(user.getActivo())) {
                long activeAdmins = userRepository.countByRoleNombreAndActivoTrue("ADMINISTRADOR");
                if (activeAdmins <= 1) {
                    throw new RuntimeException("No se puede cambiar el rol del único Administrador activo del sistema");
                }
            }

            Role newRole = roleRepository.findByNombre(req.getRol().toUpperCase())
                    .orElseThrow(() -> new RuntimeException("Rol no válido o inexistente: " + req.getRol()));

            if (Boolean.FALSE.equals(newRole.getActivo())) {
                throw new RuntimeException("No se puede asignar un rol inactivo: " + newRole.getNombre());
            }

            user.setRole(newRole);
        }

        if (req.getNombre() != null && !req.getNombre().isBlank()) {
            user.setNombre(req.getNombre().trim());
        }
        if (req.getApellido() != null) {
            user.setApellido(req.getApellido().trim());
        }
        if (req.getArea() != null && !req.getArea().isBlank()) {
            user.setArea(req.getArea().trim());
        }
        if (req.getActivo() != null) {
            if (Boolean.FALSE.equals(req.getActivo()) && "ADMINISTRADOR".equalsIgnoreCase(user.getRole().getNombre())) {
                long activeAdmins = userRepository.countByRoleNombreAndActivoTrue("ADMINISTRADOR");
                if (activeAdmins <= 1) {
                    throw new RuntimeException("No se puede desactivar al único Administrador activo del sistema");
                }
            }
            user.setActivo(req.getActivo());
        }

        User saved = userRepository.save(user);

        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        Long adminId = admin != null ? admin.getId() : null;
        String adminName = admin != null ? admin.getNombre() : "ADMIN";
        String adminRole = admin != null && admin.getRole() != null ? admin.getRole().getNombre() : "ADMINISTRADOR";

        String newRole = saved.getRole() != null ? saved.getRole().getNombre() : "";
        String newSnapshot = "Nombre: " + saved.getNombre() + ", Rol: " + newRole + ", Estado: " + saved.getEstado();

        boolean roleChanged = !prevRole.equalsIgnoreCase(newRole);
        String action = roleChanged ? "USER_ROLE_CHANGED" : "USER_UPDATED";

        auditService.logAction(
                adminId,
                adminName,
                adminRole,
                action,
                "USUARIOS",
                "User",
                String.valueOf(saved.getId()),
                "Actualización de datos para usuario " + saved.getEmail() + (roleChanged ? " (Nuevo rol: " + newRole + ")" : ""),
                prevSnapshot,
                newSnapshot,
                "EXITOSO",
                request
        );

        return new UserAdminDTO(saved);
    }

    @Transactional
    public UserAdminDTO updateUserStatus(Long id, Boolean activo, String adminEmail, HttpServletRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        if (Boolean.FALSE.equals(activo) && user.getRole() != null && "ADMINISTRADOR".equalsIgnoreCase(user.getRole().getNombre())) {
            long activeAdmins = userRepository.countByRoleNombreAndActivoTrue("ADMINISTRADOR");
            if (activeAdmins <= 1) {
                throw new RuntimeException("No se puede desactivar al único Administrador activo del sistema");
            }
        }

        String prevStatus = user.getEstado();
        user.setActivo(activo);
        User saved = userRepository.save(user);
        String newStatus = saved.getEstado();

        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        Long adminId = admin != null ? admin.getId() : null;
        String adminName = admin != null ? admin.getNombre() : "ADMIN";
        String adminRole = admin != null && admin.getRole() != null ? admin.getRole().getNombre() : "ADMINISTRADOR";

        auditService.logAction(
                adminId,
                adminName,
                adminRole,
                Boolean.TRUE.equals(activo) ? "USER_ENABLED" : "USER_DISABLED",
                "USUARIOS",
                "User",
                String.valueOf(saved.getId()),
                (Boolean.TRUE.equals(activo) ? "Activación" : "Desactivación") + " de la cuenta: " + saved.getEmail(),
                prevStatus,
                newStatus,
                "EXITOSO",
                request
        );

        return new UserAdminDTO(saved);
    }

    @Transactional
    public UserAdminDTO resetPassword(Long id, ResetPasswordRequest req, String adminEmail, HttpServletRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        String newPlainPassword = (req != null && req.getNewPassword() != null && !req.getNewPassword().isBlank())
                ? req.getNewPassword()
                : "ExporTrace" + ((int) (Math.random() * 9000) + 1000);

        user.setPasswordHash(passwordEncoder.encode(newPlainPassword));
        User saved = userRepository.save(user);

        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        Long adminId = admin != null ? admin.getId() : null;
        String adminName = admin != null ? admin.getNombre() : "ADMIN";
        String adminRole = admin != null && admin.getRole() != null ? admin.getRole().getNombre() : "ADMINISTRADOR";

        auditService.logAction(
                adminId,
                adminName,
                adminRole,
                "USER_PASSWORD_RESET",
                "SEGURIDAD",
                "User",
                String.valueOf(saved.getId()),
                "Restablecimiento de credenciales para el usuario " + saved.getEmail(),
                "HASH_ANTERIOR",
                "HASH_ACTUALIZADO_BCRYPT",
                "EXITOSO",
                request
        );

        return new UserAdminDTO(saved);
    }
}
