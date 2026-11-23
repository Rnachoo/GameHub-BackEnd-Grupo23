package com.payment.services;

import com.payment.clients.OrdenClient;
import com.payment.exceptions.PaymentException;
import com.payment.exceptions.RecursoNoEncontradoException;
import com.payment.models.Payment;
import com.payment.models.dtos.OrderDTO;
import com.payment.models.dtos.OrderUpdateEstadoDTO;
import com.payment.models.dtos.PaymentDetalleDTO;
import com.payment.models.dtos.PaymentSaveDTO;
import com.payment.models.dtos.PaymentUpdateEstadoDTO;
import com.payment.repositories.PaymentRepository;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    public static final String METODO_RECHAZO = "RECHAZO_PRUEBA";

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrdenClient ordenClient;

    @Transactional(readOnly = true)
    @Override
    public List<PaymentDetalleDTO> findAllByOrdenId(Long ordenId) {
        return this.paymentRepository.findByOrdenId(ordenId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<PaymentDetalleDTO> findAllByEstado(String estado) {
        return this.paymentRepository.findByEstado(estado.toUpperCase()).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public PaymentDetalleDTO findById(Long id) {
        return toDTO(buscar(id));
    }

    @Transactional
    @Override
    public PaymentDetalleDTO save(PaymentSaveDTO paymentSaveDTO) {
        if (paymentSaveDTO.getOrdenId() == null) {
            throw new PaymentException("El ID de la orden es obligatorio");
        }

        OrderDTO orden;
        try {
            orden = ordenClient.getOrdenById(paymentSaveDTO.getOrdenId());
        } catch (FeignException.NotFound e) {
            throw new RecursoNoEncontradoException("La orden a pagar no existe");
        } catch (FeignException e) {
            log.error("Error al consultar la orden {}", paymentSaveDTO.getOrdenId());
            throw new PaymentException("La orden a pagar no existe o el servicio no responde");
        }

        if ("CANCELADA".equals(orden.getEstado())) {
            throw new PaymentException("No se puede pagar una orden cancelada");
        }
        if (!orden.getTotal().equals(paymentSaveDTO.getMonto())) {
            throw new PaymentException("El monto de la compra debe coincidir con el total de la orden");
        }
        if ("PAGADA".equals(orden.getEstado())
                || this.paymentRepository.existsByOrdenIdAndEstado(paymentSaveDTO.getOrdenId(), "APROBADO")) {
            throw new PaymentException("Esta orden ya ha sido pagada");
        }

        boolean rechazado = METODO_RECHAZO.equalsIgnoreCase(paymentSaveDTO.getMetodo());

        Payment payment = new Payment();
        payment.setOrdenId(paymentSaveDTO.getOrdenId());
        payment.setMonto(paymentSaveDTO.getMonto());
        payment.setMetodo(paymentSaveDTO.getMetodo());
        payment.setEstado(rechazado ? "RECHAZADO" : "APROBADO");
        payment.setCodigoTransaccion(UUID.randomUUID().toString());
        payment.setFecha(LocalDateTime.now());
        payment = paymentRepository.save(payment);
        log.info("Pago {} registrado como {}", payment.getId(), payment.getEstado());

        if (!rechazado) {
            actualizarOrden(orden.getId(), "PAGADA",
                    "El pago fue procesado, pero no se pudo actualizar el estado de la orden. Operación cancelada.");
        }
        return toDTO(payment);
    }

    @Transactional
    @Override
    public PaymentDetalleDTO updateEstado(Long id, PaymentUpdateEstadoDTO paymentUpdateEstadoDTO) {
        Payment payment = buscar(id);
        String nuevoEstado = paymentUpdateEstadoDTO.getEstado().toUpperCase();

        if ("APROBADO".equals(nuevoEstado)) {
            if (!"APROBADO".equals(payment.getEstado())
                    && this.paymentRepository.existsByOrdenIdAndEstado(payment.getOrdenId(), "APROBADO")) {
                throw new PaymentException("La orden ya tiene un pago aprobado");
            }
            actualizarOrden(payment.getOrdenId(), "PAGADA", "No se pudo actualizar el estado de la orden");
        } else if ("RECHAZADO".equals(nuevoEstado)) {
            if ("APROBADO".equals(payment.getEstado())) {
                actualizarOrden(payment.getOrdenId(), "PENDIENTE_PAGO", "No se pudo actualizar el estado de la orden");
            }
        } else {
            throw new PaymentException("Estado no valido. Use APROBADO o RECHAZADO, o anule el pago");
        }

        payment.setEstado(nuevoEstado);
        log.info("Pago {} actualizado a {}", id, nuevoEstado);
        return toDTO(this.paymentRepository.save(payment));
    }

    @Transactional
    @Override
    public PaymentDetalleDTO nullById(Long id) {
        Payment payment = buscar(id);
        if ("ANULADO".equals(payment.getEstado())) {
            throw new PaymentException("El pago ya se encuentra anulado");
        }
        if ("APROBADO".equals(payment.getEstado())) {
            actualizarOrden(payment.getOrdenId(), "CANCELADA", "No se pudo cancelar la orden del pago");
        }
        payment.setEstado("ANULADO");
        log.info("Pago {} anulado", id);
        return toDTO(paymentRepository.save(payment));
    }

    private void actualizarOrden(Long ordenId, String estado, String mensajeError) {
        try {
            OrderUpdateEstadoDTO estadoDTO = new OrderUpdateEstadoDTO();
            estadoDTO.setEstado(estado);
            ordenClient.actualizarEstadoOrden(ordenId, estadoDTO);
        } catch (FeignException e) {
            log.error("No se pudo actualizar la orden {} a {}", ordenId, estado);
            throw new PaymentException(mensajeError);
        }
    }

    private Payment buscar(Long id) {
        return this.paymentRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago con ID " + id + " no encontrado"));
    }

    private PaymentDetalleDTO toDTO(Payment payment) {
        PaymentDetalleDTO dto = new PaymentDetalleDTO();
        dto.setId(payment.getId());
        dto.setOrdenId(payment.getOrdenId());
        dto.setMonto(payment.getMonto());
        dto.setMetodo(payment.getMetodo());
        dto.setEstado(payment.getEstado());
        dto.setCodigoTransaccion(payment.getCodigoTransaccion());
        dto.setFecha(payment.getFecha());
        return dto;
    }
}
