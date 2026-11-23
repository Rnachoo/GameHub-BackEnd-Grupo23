package com.inventory.services;

import com.inventory.clients.ProductClient;
import com.inventory.exceptions.InventoryException;
import com.inventory.models.Inventory;
import com.inventory.models.MovimientoInventario;
import com.inventory.models.OperacionStockOrden;
import com.inventory.models.dtos.ConfirmacionStockDTO;
import com.inventory.models.dtos.InventoryCantidadDTO;
import com.inventory.models.dtos.InventoryDetalleDTO;
import com.inventory.models.dtos.InventorySaveDTO;
import com.inventory.models.dtos.InventoryUpdateCantidadDisponibleDTO;
import com.inventory.models.dtos.MovimientoDetalleDTO;
import com.inventory.models.dtos.ProductDTO;
import com.inventory.repositories.InventoryRepository;
import com.inventory.repositories.OperacionStockOrdenRepository;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private OperacionStockOrdenRepository operacionStockOrdenRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Inventory inventarioPrueba;

    @BeforeEach
    void setUp() {
        inventarioPrueba = new Inventory();
        inventarioPrueba.setId(1L);
        inventarioPrueba.setProductId(100L);
        inventarioPrueba.setStockDisponible(50L);
        inventarioPrueba.setStockReservado(10L);
        inventarioPrueba.setStockMinimo(5L);
        inventarioPrueba.setUbicacion("Pasillo 3, Estante A");

        MovimientoInventario entrada = new MovimientoInventario();
        entrada.setId(1L);
        entrada.setProductId(100L);
        entrada.setTipo("ENTRADA");
        entrada.setCantidad(50L);
        entrada.setFecha(LocalDateTime.of(2026, 11, 23, 12, 0));
        entrada.setInventario(inventarioPrueba);
        inventarioPrueba.getMovimientos().add(entrada);
    }

    @Test
    @DisplayName("Lista el inventario del producto con sus movimientos")
    void shouldFindAllByProduct() {
        when(inventoryRepository.findByProductId(100L))
                .thenReturn(List.of(inventarioPrueba));

        List<InventoryDetalleDTO> resultado = inventoryService.findAllByProduct(100L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getProductId()).isEqualTo(100L);
        assertThat(resultado.get(0).getMovimientosDTO()).hasSize(1);
        verify(inventoryRepository).findByProductId(100L);
    }

    @Test
    @DisplayName("Consulta el inventario por ID")
    void shouldFindById() {
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventarioPrueba));

        InventoryDetalleDTO resultado = inventoryService.findByID(1L);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getStockDisponible()).isEqualTo(50L);
        verify(inventoryRepository).findById(1L);
    }

    @Test
    @DisplayName("Crea el inventario tras validar el producto")
    void shouldSaveInventory() {
        InventorySaveDTO solicitud = new InventorySaveDTO();
        solicitud.setProductId(100L);
        solicitud.setStockDisponible(50L);
        solicitud.setStockReservado(0L);
        solicitud.setStockMinimo(5L);
        solicitud.setUbicacion("Almacén Central");

        MovimientoDetalleDTO entrada = new MovimientoDetalleDTO();
        entrada.setProductId(100L);
        entrada.setTipo("ENTRADA");
        entrada.setCantidad(50L);
        solicitud.setMovimientosDTO(List.of(entrada));

        ProductDTO producto = new ProductDTO();
        producto.setId(100L);
        when(productClient.getProductById(100L)).thenReturn(producto);
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocacion -> {
            Inventory inventario = invocacion.getArgument(0);
            inventario.setId(2L);
            return inventario;
        });

        InventoryDetalleDTO resultado = inventoryService.save(solicitud);

        assertThat(resultado.getId()).isEqualTo(2L);
        assertThat(resultado.getMovimientosDTO()).hasSize(1);
        verify(productClient).getProductById(100L);
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Rechaza la creación si falla la consulta del producto")
    void shouldThrowExceptionWhenProductClientFails() {
        InventorySaveDTO solicitud = new InventorySaveDTO();
        solicitud.setProductId(999L);

        when(productClient.getProductById(999L)).thenThrow(mock(FeignException.class));

        assertThatThrownBy(() -> inventoryService.save(solicitud))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("no existe o el servicio no está disponible");

        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Ajusta el stock disponible y registra la diferencia")
    void shouldUpdateCantidadDisponible() {
        InventoryUpdateCantidadDisponibleDTO ajuste = new InventoryUpdateCantidadDisponibleDTO();
        ajuste.setStockDisponible(80L);

        when(inventoryRepository.findInventarioById(1L))
                .thenReturn(Optional.of(inventarioPrueba));
        when(inventoryRepository.save(inventarioPrueba)).thenReturn(inventarioPrueba);

        InventoryDetalleDTO resultado = inventoryService.updateCantidadDisponible(1L, ajuste);

        assertThat(resultado.getStockDisponible()).isEqualTo(80L);
        assertThat(resultado.getStockReservado()).isEqualTo(10L);
        assertThat(resultado.getMovimientosDTO()).hasSize(2);
        assertThat(resultado.getMovimientosDTO().get(1).getTipo()).isEqualTo("AJUSTE_ENTRADA");
        assertThat(resultado.getMovimientosDTO().get(1).getCantidad()).isEqualTo(30L);
        verify(inventoryRepository).save(inventarioPrueba);
    }

    @Test
    @DisplayName("Rechaza un ajuste de stock negativo")
    void shouldThrowExceptionWhenUpdateNegativeStock() {
        InventoryUpdateCantidadDisponibleDTO ajuste = new InventoryUpdateCantidadDisponibleDTO();
        ajuste.setStockDisponible(-10L);

        when(inventoryRepository.findInventarioById(1L))
                .thenReturn(Optional.of(inventarioPrueba));

        assertThatThrownBy(() -> inventoryService.updateCantidadDisponible(1L, ajuste))
                .isInstanceOf(InventoryException.class)
                .hasMessage("El stock disponible no puede quedar en negativo.");

        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Elimina el inventario si no tiene reservas")
    void shouldDeleteById() {
        inventarioPrueba.setStockReservado(0L);
        when(inventoryRepository.findInventarioById(1L))
                .thenReturn(Optional.of(inventarioPrueba));

        inventoryService.deleteById(1L);

        verify(inventoryRepository).delete(inventarioPrueba);
    }

    @Test
    @DisplayName("Impide eliminar un inventario con unidades reservadas")
    void shouldThrowExceptionWhenDeleteWithReservedStock() {
        when(inventoryRepository.findInventarioById(1L))
                .thenReturn(Optional.of(inventarioPrueba));

        assertThatThrownBy(() -> inventoryService.deleteById(1L))
                .isInstanceOf(InventoryException.class)
                .hasMessage("No se puede eliminar el inventario dado que hay productos reservados");

        verify(inventoryRepository, never()).delete(any(Inventory.class));
    }

    @Test
    @DisplayName("Reservar mueve unidades de disponible a reservado")
    void shouldReservarStock() {
        prepararEscrituraPorProducto();

        InventoryDetalleDTO resultado = inventoryService.reservarStock(100L, cantidad(10L));

        assertThat(resultado.getStockDisponible()).isEqualTo(40L);
        assertThat(resultado.getStockReservado()).isEqualTo(20L);
        verify(inventoryRepository).save(inventarioPrueba);
    }

    @Test
    @DisplayName("Rechaza una reserva superior al stock disponible")
    void shouldThrowExceptionWhenReservarStockInsufficient() {
        when(inventoryRepository.findInventarioByProductId(100L))
                .thenReturn(Optional.of(inventarioPrueba));

        assertThatThrownBy(() -> inventoryService.reservarStock(100L, cantidad(100L)))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("Stock insuficiente");

        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Confirmar una venta descuenta solo del reservado")
    void shouldConfirmarVenta() {
        prepararEscrituraPorProducto();

        InventoryDetalleDTO resultado = inventoryService.confirmarVenta(100L, cantidad(5L));

        assertThat(resultado.getStockDisponible()).isEqualTo(50L);
        assertThat(resultado.getStockReservado()).isEqualTo(5L);
        assertThat(resultado.getMovimientosDTO().get(1).getTipo()).isEqualTo("VENTA");
        verify(inventoryRepository).save(inventarioPrueba);
    }

    @Test
    @DisplayName("Liberar una reserva devuelve unidades al disponible")
    void shouldLiberarStock() {
        prepararEscrituraPorProducto();

        InventoryDetalleDTO resultado = inventoryService.liberarStock(100L, cantidad(5L));

        assertThat(resultado.getStockDisponible()).isEqualTo(55L);
        assertThat(resultado.getStockReservado()).isEqualTo(5L);
        verify(inventoryRepository).save(inventarioPrueba);
    }

    @Test
    @DisplayName("Repetir la aprobación conserva las unidades reservadas para otra orden")
    void confirmarDosVecesConservaLaOtraReserva() {
        // Cuatro reservadas: dos de esta orden y dos de otra.
        inventarioPrueba.setStockDisponible(6L);
        inventarioPrueba.setStockReservado(4L);

        ConfirmacionStockDTO solicitud = new ConfirmacionStockDTO(
                1L,
                List.of(new ConfirmacionStockDTO.Linea(100L, 2L))
        );

        OperacionStockOrden registrada = new OperacionStockOrden(
                200L, 1L, "PENDIENTE_PAGO", "PAGADA"
        );

        when(inventoryRepository.findInventarioByProductId(100L))
                .thenReturn(Optional.of(inventarioPrueba));
        when(operacionStockOrdenRepository.findByOrdenIdAndRevision(200L, 1L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(registrada));

        inventoryService.confirmarStockOrden(200L, solicitud);
        inventoryService.confirmarStockOrden(200L, solicitud);

        assertThat(inventarioPrueba.getStockDisponible()).isEqualTo(6L);
        assertThat(inventarioPrueba.getStockReservado()).isEqualTo(2L);

        // Una entrada inicial y una sola venta.
        assertThat(inventarioPrueba.getMovimientos()).hasSize(2);
        MovimientoInventario venta = inventarioPrueba.getMovimientos().get(1);
        assertThat(venta.getTipo()).isEqualTo("VENTA");
        assertThat(venta.getCantidad()).isEqualTo(2L);

        ArgumentCaptor<OperacionStockOrden> captor =
                ArgumentCaptor.forClass(OperacionStockOrden.class);
        verify(operacionStockOrdenRepository).saveAndFlush(captor.capture());

        OperacionStockOrden guardada = captor.getValue();
        assertThat(guardada.getOrdenId()).isEqualTo(200L);
        assertThat(guardada.getRevision()).isEqualTo(1L);
        assertThat(guardada.getEstadoOrigen()).isEqualTo("PENDIENTE_PAGO");
        assertThat(guardada.getEstadoDestino()).isEqualTo("PAGADA");
    }

    @Test
    @DisplayName("Si falta reserva en una línea, ninguna línea se confirma")
    void rechazaElLoteCompletoSiFaltaReserva() {
        Inventory segundoProducto = new Inventory();
        segundoProducto.setProductId(101L);
        segundoProducto.setStockDisponible(8L);
        segundoProducto.setStockReservado(1L);

        ConfirmacionStockDTO solicitud = new ConfirmacionStockDTO(
                1L,
                List.of(
                        new ConfirmacionStockDTO.Linea(100L, 2L),
                        new ConfirmacionStockDTO.Linea(101L, 3L)
                )
        );

        when(inventoryRepository.findInventarioByProductId(100L))
                .thenReturn(Optional.of(inventarioPrueba));
        when(inventoryRepository.findInventarioByProductId(101L))
                .thenReturn(Optional.of(segundoProducto));
        when(operacionStockOrdenRepository.findByOrdenIdAndRevision(200L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.confirmarStockOrden(200L, solicitud))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("101");

        assertThat(inventarioPrueba.getStockReservado()).isEqualTo(10L);
        assertThat(inventarioPrueba.getMovimientos()).hasSize(1);
        assertThat(segundoProducto.getStockReservado()).isEqualTo(1L);
        assertThat(segundoProducto.getMovimientos()).isEmpty();
        verify(operacionStockOrdenRepository, never())
                .saveAndFlush(any(OperacionStockOrden.class));
    }

    private void prepararEscrituraPorProducto() {
        when(inventoryRepository.findInventarioByProductId(100L))
                .thenReturn(Optional.of(inventarioPrueba));
        when(inventoryRepository.save(inventarioPrueba)).thenReturn(inventarioPrueba);
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventarioPrueba));
    }

    private InventoryCantidadDTO cantidad(long unidades) {
        InventoryCantidadDTO dto = new InventoryCantidadDTO();
        dto.setCantidad(unidades);
        return dto;
    }
}