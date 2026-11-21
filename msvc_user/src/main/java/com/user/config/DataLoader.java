package com.user.config;

import com.user.models.Direction;
import com.user.models.User;
import com.user.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Value("${seed.password:}")
    private String seedPassword;

    @Override
    public void run(String... args) {
        if (seedPassword.isBlank()) {
            return;
        }
        crearPerfil("Administrador GameHub", "admin@gamehub.cl", "911111111", false);
        crearPerfil("Operador GameHub", "operador@gamehub.cl", "922222222", false);
        crearPerfil("Cliente Demo", "cliente@gamehub.cl", "933333333", true);
    }

    private void crearPerfil(String nombre, String email, String telefono, boolean conDireccion) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User user = new User();
        user.setNombreUser(nombre);
        user.setEmail(email);
        user.setTelefono(telefono);
        user.setEstado("Active");

        if (conDireccion) {
            Direction direccion = new Direction();
            direccion.setComuna("Providencia");
            direccion.setCiudad("Región Metropolitana");
            direccion.setCalle("Av. Providencia");
            direccion.setNumero("1234");
            direccion.setUser(user);
            user.getDirections().add(direccion);
        }
        userRepository.save(user);
    }
}