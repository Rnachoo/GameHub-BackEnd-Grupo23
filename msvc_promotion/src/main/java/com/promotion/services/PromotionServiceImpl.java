package com.promotion.services;

import com.promotion.clients.CategoryClient;
import com.promotion.exceptions.PromotionException;
import com.promotion.exceptions.RecursoNoEncontradoException;
import com.promotion.models.Promotion;
import com.promotion.models.dtos.PromotionAplicarDescuentoDTO;
import com.promotion.models.dtos.PromotionDetalleDTO;
import com.promotion.models.dtos.PromotionSaveDTO;
import com.promotion.models.dtos.PromotionUpdateDateDTO;
import com.promotion.repositories.PromotionRepository;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class PromotionServiceImpl implements PromotionService {
    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private CategoryClient categoryClient;

    @Transactional(readOnly = true)
    @Override
    public List<PromotionDetalleDTO> findAll() {
        log.info("Abriendo lista de historial de promociones");
        return this.promotionRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<PromotionDetalleDTO> findCurrent() {
        log.info("Abriendo lista de promociones activas");
        return this.promotionRepository.findByEstado("Active").stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public PromotionDetalleDTO findById(Long id) {
        return toDTO(buscar(id));
    }

    @Transactional(readOnly = true)
    @Override
    public PromotionDetalleDTO findByCodigo(String codigo) {
        Promotion promotion = this.promotionRepository.findByCodigo(codigo.trim().toUpperCase()).orElseThrow(
                () -> new RecursoNoEncontradoException("Promocion con codigo " + codigo + " no encontrado"));
        return toDTO(promotion);
    }

    @Transactional
    @Override
    public PromotionDetalleDTO save(PromotionSaveDTO promotionSaveDTO) {
        if (this.promotionRepository.findByCodigo(promotionSaveDTO.getCodigo()).isPresent()) {
            throw new PromotionException("Promocion con codigo " + promotionSaveDTO.getCodigo() + " ya está registrada");
        }
        if (promotionSaveDTO.getCategoryId() != null) {
            try {
                categoryClient.getCategoryById(promotionSaveDTO.getCategoryId());
            } catch (FeignException e) {
                log.error("Error al consultar category-service para el ID " + promotionSaveDTO.getCategoryId());
                throw new PromotionException("La categoría ingresada no existe o el servicio no está disponible");
            }
        }

        LocalDateTime fechaInicio = promotionSaveDTO.getFechaInicio() != null
                ? promotionSaveDTO.getFechaInicio()
                : LocalDateTime.now();
        validarFechas(fechaInicio, promotionSaveDTO.getFechaFin());
        if ("PORCENTAJE".equalsIgnoreCase(promotionSaveDTO.getTipo()) && promotionSaveDTO.getValor() > 100) {
            throw new PromotionException("Un descuento porcentual no puede superar el 100 %");
        }

        Promotion promotion = new Promotion();
        promotion.setCodigo(promotionSaveDTO.getCodigo().trim().toUpperCase());
        promotion.setValor(promotionSaveDTO.getValor());
        promotion.setTipo(promotionSaveDTO.getTipo().trim().toUpperCase());
        promotion.setFechaInicio(fechaInicio);
        promotion.setFechaFin(promotionSaveDTO.getFechaFin());
        promotion.setMontoMinimo(promotionSaveDTO.getMontoMinimo());
        promotion.setUsosMaximos(promotionSaveDTO.getUsosMaximos());
        promotion.setTope(promotionSaveDTO.getTope());
        promotion.setEstado("Active");

        promotion = promotionRepository.save(promotion);
        log.info("Promocion con codigo " + promotion.getCodigo() + " creada con exito!");
        return toDTO(promotion);
    }

    @Transactional
    @Override
    public PromotionDetalleDTO updateDate(Long id, PromotionUpdateDateDTO promotionUpdateDateDTO) {
        Promotion promotion = this.promotionRepository.findById(id).orElseThrow(
                () -> new RecursoNoEncontradoException("Promocion no encontrada"));
        validarFechas(promotionUpdateDateDTO.getFechaInicio(), promotionUpdateDateDTO.getFechaFin());

        promotion.setFechaInicio(promotionUpdateDateDTO.getFechaInicio());
        promotion.setFechaFin(promotionUpdateDateDTO.getFechaFin());
        promotion = this.promotionRepository.save(promotion);
        log.info("Fecha de inicio y fin actualizada con exito!");
        return toDTO(promotion);
    }

    @Transactional
    @Override
    public PromotionDetalleDTO desactiveById(Long id) {
        Promotion promotion = buscar(id);
        promotion.setEstado("Inactive");
        promotion = promotionRepository.save(promotion);
        log.info("Promocion con ID " + id + " ha sido desactivada");
        return toDTO(promotion);
    }

    @Transactional
    @Override
    public PromotionDetalleDTO aplicarPromocion(String codigo, PromotionAplicarDescuentoDTO aplicarDescuentoDTO, Double totalOrden) {
        Promotion promotion = this.promotionRepository.findByCodigo(codigo).orElseThrow(
                () -> new RecursoNoEncontradoException("El cupon " + codigo + " no existe"));
        if (!"Active".equalsIgnoreCase(promotion.getEstado())) {
            throw new PromotionException("El cupon no está activo");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(promotion.getFechaInicio()) || now.isAfter(promotion.getFechaFin())) {
            throw new PromotionException("El cupon está vencido o aún no es valido para esta fecha");
        }
        if (totalOrden < promotion.getMontoMinimo()) {
            throw new PromotionException("El total de la orden no alcanza el mínimo requerido para usar este cupón");
        }
        if (promotion.getUsosMaximos() == null || promotion.getUsosMaximos() <= 0) {
            throw new PromotionException("El cupon ha alcanzado su limite de usos maximos");
        }

        promotion.setUsosMaximos(promotion.getUsosMaximos() - 1);
        promotion = promotionRepository.save(promotion);
        log.info("cupon " + codigo + " aplicado exitosamente, Usos restantes: " + promotion.getUsosMaximos());
        return toDTO(promotion);
    }

    private Promotion buscar(Long id) {
        return this.promotionRepository.findById(id).orElseThrow(
                () -> new RecursoNoEncontradoException("Promocion con ID " + id + " no encontrada"));
    }

    private void validarFechas(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null || fin == null) {
            throw new PromotionException("Las fechas de inicio y término son obligatorias");
        }
        if (fin.isBefore(inicio)) {
            throw new PromotionException("La fecha de término no puede ser anterior a la fecha de inicio");
        }
    }

    private PromotionDetalleDTO toDTO(Promotion promotion) {
        PromotionDetalleDTO dto = new PromotionDetalleDTO();
        dto.setId(promotion.getId());
        dto.setCodigo(promotion.getCodigo());
        dto.setValor(promotion.getValor());
        dto.setTipo(promotion.getTipo());
        dto.setFechaInicio(promotion.getFechaInicio());
        dto.setFechaFin(promotion.getFechaFin());
        dto.setMontoMinimo(promotion.getMontoMinimo());
        dto.setUsosMaximos(promotion.getUsosMaximos());
        dto.setTope(promotion.getTope());
        dto.setEstado(promotion.getEstado());
        return dto;
    }
}
