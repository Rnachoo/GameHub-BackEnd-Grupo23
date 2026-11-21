package com.auth.config;

import com.auth.models.Rol;
import com.auth.repositories.RolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private RolRepository rolRepository;

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
    }
}