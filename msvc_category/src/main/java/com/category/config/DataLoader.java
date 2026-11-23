package com.category.config;

import com.category.models.Category;
import com.category.repositories.CategoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataLoader implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    public DataLoader(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            return;
        }
        crear("Notebooks", "Equipos preparados para jugar y trabajar");
        crear("Tarjetas gráficas", "Potencia visual para tus juegos");
        crear("Procesadores", "Rendimiento para cada tarea");
        crear("Periféricos", "Teclados, mouse y dispositivos gamer");
        crear("Consolas", "Todo lo necesario para jugar");
        crear("Monitores", "Velocidad y nitidez en cada partida");
        crear("Accesorios", "Complementos para tu espacio gamer");
        log.info("Seed de categorías cargado: {} categorías", categoryRepository.count());
    }

    private void crear(String nombre, String descripcion) {
        Category category = new Category();
        category.setNombreCategory(nombre);
        category.setDescripcion(descripcion);
        category.setEstado("Active");
        categoryRepository.save(category);
    }
}
