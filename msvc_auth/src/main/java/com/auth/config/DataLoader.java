package com.auth.config;

import com.auth.models.Auth;
import com.auth.models.Rol;
import com.auth.repositories.AuthRepository;
import com.auth.repositories.RolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Si no está definida, no se crea ninguna cuenta.
    @Value("${seed.password:}")
    private String seedPassword;

    @Override
    public void run(String... args) throws Exception {
        // Crea los roles que no existan: cliente, administrador y operador
        for (String nombre : List.of("ROLE_USER", "ROLE_ADMIN", "ROLE_OPERADOR")) {
            if (rolRepository.findByNombre(nombre).isEmpty()) {
                Rol rol = new Rol();
                rol.setNombre(nombre);
                rolRepository.save(rol);
            }
        }

        if (seedPassword.isBlank()) {
            return;
        }
        crearCuenta("admin", "admin@gamehub.cl", "ROLE_ADMIN");
        crearCuenta("operador", "operador@gamehub.cl", "ROLE_OPERADOR");
        crearCuenta("cliente", "cliente@gamehub.cl", "ROLE_USER");
    }

    //si la cuenta ya existe no la vuelve a crear
    private void crearCuenta(String nombreCuenta, String email, String nombreRol) {
        if (authRepository.findByEmail(email).isPresent()) {
            return;
        }
        Rol rol = rolRepository.findByNombre(nombreRol).orElseThrow();
        Auth auth = new Auth();
        auth.setNombreCuenta(nombreCuenta);
        auth.setEmail(email);
        auth.setPassword(passwordEncoder.encode(seedPassword));
        auth.setEstado("Active");
        auth.setFechaCreacion(LocalDateTime.now());
        auth.getRoles().add(rol);
        authRepository.save(auth);
    }
}