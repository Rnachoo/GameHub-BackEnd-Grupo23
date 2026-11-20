package com.product.config;

import com.product.models.Product;
import com.product.repositories.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DataLoader implements CommandLineRunner {

    private final ProductRepository productRepository;

    public DataLoader(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            return;
        }
        crear("Mouse Logitech g203", "Logitech", 49990L, 39990L, true, 4L,
                "Mouse gamer de alta precisión con iluminación RGB.",
                List.of("https://casaroyal.vtexassets.com/arquivos/ids/163607-1200-1200?v=638874964810800000&width=1200&height=1200&aspect=true"),
                List.of("Sensor óptico de 16000 DPI",
                        "Cable trenzado de 1.8 metros",
                        "Seis botones programables",
                        "Iluminación RGB"));
        crear("Notebook Lenovo Legion 5", "Lenovo", 999990L, 899990L, true, 1L,
                "Notebook gamer equilibrado para jugar, estudiar y trabajar.",
                List.of("https://home.ripley.cl/store/Attachment/WOP/D113/2000385918295/2000385918295_2.jpg"),
                List.of("Procesador AMD Ryzen 5",
                        "16 GB de memoria RAM",
                        "Almacenamiento SSD de 512 GB"));
        crear("Tarjeta Gráfica GeForce RTX 4060", "NVIDIA", 429990L, 399990L, true, 2L,
                "Tarjeta gráfica preparada para ejecutar juegos en alta resolución.",
                List.of("https://media.solotodo.com/media/products/1776795_picture_1688389515.jpg"),
                List.of("8 GB de memoria GDDR6",
                        "Soporte para RayTracing",
                        "Consumo energetico 120W"));
        crear("Procesador AMD Ryzen 7 5700X", "AMD", 219990L, 189990L, false, 3L,
                "Procesador de ocho núcleos para juegos y tareas exigentes.",
                List.of("https://media.solotodo.com/media/products/1569342_picture_1672268351.jpg"),
                List.of("8 núcleos y 16 hilos",
                        "Frecuencia máxima de 4.6 GHz",
                        "Socket AM4"));
        crear("PlayStation 5 Slim", "Sony", 599990L, 549990L, true, 5L,
                "Consola de última generación con almacenamiento SSD.",
                List.of("https://www.weplay.cl/pub/media/catalog/product/cache/3f1b140c3c9f36fbf6b01dffb521c246/4/9/4948872415910-01.jpg"),
                List.of("Almacenamiento SSD de alta velocidad",
                        "Resolución máxima 4K",
                        "Control inalámbrico incluido"));
        crear("Monitor Samsung Odyssey G3", "Samsung", 199990L, null, false, 6L,
                "Monitor Full HD diseñado para juegos competitivos.",
                List.of("https://media.falabella.com/falabellaCL/150196666_01/w=1500,h=1500,fit=cover"),
                List.of("Resolución Full HD",
                        "Frecuencia de actualización de 144 Hz",
                        "Pantalla de 24 pulgadas"));
        crear("Audífonos Logitech G733", "Logitech", 74990L, 64990L, false, 7L,
                "Audífonos envolventes con micrófono.",
                List.of("https://www.lifemaxstore.cl/cdn/shop/files/p-981-000889-1-c1b65656-e38a-4a08-aaeb-86ecc5f36f77.jpg?v=1774300914"),
                List.of("Sonido envolvente virtual 7.1",
                        "Ultraligeros",
                        "Inalambricos"));
        log.info("Seed de productos cargado: {} productos", productRepository.count());
    }

    private void crear(String nombre, String marca, Long precio, Long precioDescuento, boolean destacado,
                       Long categoriaId, String descripcion, List<String> imagenes, List<String> especificaciones) {
        Product product = new Product();
        product.setNombre(nombre);
        product.setMarca(marca);
        product.setPrecio(precio);
        product.setPrecioDescuento(precioDescuento);
        product.setDestacado(destacado);
        product.setCategoriaId(categoriaId);
        product.setDescripcion(descripcion);
        product.setEstado("Activo");
        product.getImagenes().addAll(imagenes);
        product.getEspecificaciones().addAll(especificaciones);
        productRepository.save(product);
    }
}
