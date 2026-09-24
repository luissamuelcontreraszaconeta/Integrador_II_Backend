package com.exportrace.service;

import com.exportrace.dto.UserDTO;
import com.exportrace.entity.Role;
import com.exportrace.entity.User;
import com.exportrace.repository.RoleRepository;
import com.exportrace.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream().map(UserDTO::new).toList();
    }

    public UserDTO createUser(User user, String roleName) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("El correo ya se encuentra registrado: " + user.getEmail());
        }

        Role role = roleRepository.findByNombre(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName, "Rol " + roleName)));

        user.setRole(role);
        user.setContrasena(passwordEncoder.encode(user.getContrasena()));
        user.setEstado("ACTIVO");

        User saved = userRepository.save(user);
        return new UserDTO(saved);
    }
}
