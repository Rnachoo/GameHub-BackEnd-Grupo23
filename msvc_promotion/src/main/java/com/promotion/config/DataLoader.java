package com.promotion.config;

import com.promotion.models.Promotion;
import com.promotion.repositories.PromotionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class DataLoader implements CommandLineRunner {

    private final PromotionRepository promotionRepository;

    public DataLoader(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    @Override
    public void run(String... args) {
        if (promotionRepository.count() > 0) {
            return;
        }
        crear("GAMER10", 10, 15000.0, LocalDateTime.of(2027, 12, 31, 23, 59));
        crear("BIENVENIDO15", 15, 20000.0, LocalDateTime.of(2027, 12, 31, 23, 59));
        crear("GAMEHUB5", 5, null, LocalDateTime.of(2028, 12, 31, 23, 59));
        log.info("Seed de cupones cargado: {} cupones", promotionRepository.count());
    }

    private void crear(String codigo, double porcentaje, Double tope, LocalDateTime fechaFin) {
        Promotion promotion = new Promotion();
        promotion.setCodigo(codigo);
        promotion.setTipo("PORCENTAJE");
        promotion.setValor(porcentaje);
        promotion.setTope(tope);
        promotion.setFechaInicio(LocalDateTime.of(2024, 1, 1, 0, 0));
        promotion.setFechaFin(fechaFin);
        promotion.setMontoMinimo(0);
        promotion.setUsosMaximos(100L);
        promotion.setEstado("Active");
        promotionRepository.save(promotion);
    }
}
