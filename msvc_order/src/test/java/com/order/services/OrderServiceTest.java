package com.order.services;

import com.order.clients.InventoryClient;
import com.order.clients.ProductClient;
import com.order.clients.PromotionClient;
import com.order.clients.UserClient;
import com.order.exceptions.OrderException;
import com.order.exceptions.RecursoNoEncontradoException;
import com.order.models.Order;
import com.order.models.OrderItem;
import com.order.models.dtos.*;
import com.order.repositories.OrderRepository;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private ProductClient productClient;

    @Mock
    private PromotionClient promotionClient;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order ordenPrueba;
    private UserDTO usuario;
    private ProductDTO producto;
    private InventoryDTO inventario;

    @BeforeEach
    void setUp() {
        usuario = new UserDTO();
        usuario.setId(1L);
        usuario.setNombreUser("Usuario de prueba");

        producto = new ProductDTO();
        producto.setId(10L);
        producto.setPrecio(15000L);
        producto.setEstado("Activo");

        inventario = new InventoryDTO();
        inventario.setProductId(10L);
        inventario.setStockDisponible(50L);

        ordenPrueba = new Order();
        ordenPrueba.setId(100L);
        ordenPrueba.setUserId(1L);
        ordenPrueba.setFecha(LocalDateTime.of(2026, 11, 23, 12, 0));
        ordenPrueba.setEstado("PENDIENTE_PAGO");
        ordenPrueba.setSubtotal(30000L);
        ordenPrueba.setDescuento(0L);
        ordenPrueba.setTotal(30000L);

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setProductId(10L);
        item.setCantidad(2L);
        item.setPrecioUnitario(15000L);
        item.setOrder(ordenPrueba);
        ordenPrueba.getItems().add(item);
    }

    @Test
    @DisplayName("Lista las órdenes del cliente con sus productos")
    void shouldFindByClient() {
        when(orderRepository.findByUserId(1L)).thenReturn(List.of(ordenPrueba));

        List<OrderDetalleDTO> resultado = orderService.findByClient(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getUserId()).isEqualTo(1L);
        assertThat(resultado.get(0).getItems()).hasSize(1);
        verify(orderRepository).findByUserId(1L);
    }

    @Test
    @DisplayName("Lista las órdenes por estado")
    void shouldFindByEstado() {
        when(orderRepository.findByEstado("PENDIENTE_PAGO"))
                .thenReturn(List.of(ordenPrueba));

        List<OrderDetalleDTO> resultado = orderService.findByEstado("PENDIENTE_PAGO");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEstado()).isEqualTo("PENDIENTE_PAGO");
        verify(orderRepository).findByEstado("PENDIENTE_PAGO");
    }

    @Test
    @DisplayName("Busca una orden por ID sin solicitar bloqueo de escritura")
    void shouldFindById() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(ordenPrueba));

        OrderDetalleDTO resultado = orderService.findById(100L);

        assertThat(resultado.getId()).isEqualTo(100L);
        verify(orderRepository).findById(100L);
        verify(orderRepository, never()).findOrdenById(anyLong());
    }

    @Test
    @DisplayName("Informa cuando la orden solicitada no existe")
    void shouldThrowExceptionWhenOrderNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(999L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("La orden con id 999 no existe");
    }

    @Test
    @DisplayName("Guarda la orden, reserva stock y aplica la promoción")
    void shouldSaveOrderSuccessfully() {
        OrderSaveDTO solicitud = solicitudDeOrden();
        solicitud.setCodigoPromocion("DESCUENTO_VERANO");

        PromotionDTO promocion = new PromotionDTO();
        promocion.setValor(5000.0);

        when(userClient.getUserById(1L)).thenReturn(usuario);
        when(productClient.getProductById(10L)).thenReturn(producto);
        when(inventoryClient.reservarStock(eq(10L), any(InventoryCantidadDTO.class)))
                .thenReturn(inventario);
        when(promotionClient.aplicarPromocion(
                eq("DESCUENTO_VERANO"), any(PromotionSaveDTO.class), eq(30000.0)
        )).thenReturn(promocion);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocacion -> {
            Order orden = invocacion.getArgument(0);
            orden.setId(200L);
            return orden;
        });

        OrderDetalleDTO resultado = orderService.save(solicitud);

        assertThat(resultado.getId()).isEqualTo(200L);
        assertThat(resultado.getSubtotal()).isEqualTo(30000L);
        assertThat(resultado.getDescuento()).isEqualTo(5000L);
        assertThat(resultado.getTotal()).isEqualTo(25000L);
        verify(userClient).getUserById(1L);
        verify(productClient).getProductById(10L);
        verify(inventoryClient).reservarStock(eq(10L), any(InventoryCantidadDTO.class));
        verify(promotionClient).aplicarPromocion(
                eq("DESCUENTO_VERANO"), any(PromotionSaveDTO.class), eq(30000.0)
        );
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Rechaza la creación si el usuario no existe")
    void shouldThrowExceptionWhenUserNotExistsOnSave() {
        OrderSaveDTO solicitud = solicitudDeOrden();
        solicitud.setUserId(999L);

        when(userClient.getUserById(999L)).thenThrow(mock(FeignException.class));

        assertThatThrownBy(() -> orderService.save(solicitud))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("El user con id 999 no existe");

        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(inventoryClient);
    }

    @Test
    @DisplayName("Rechaza una orden con un producto inactivo")
    void shouldThrowExceptionWhenProductIsInactive() {
        OrderSaveDTO solicitud = solicitudDeOrden();
        producto.setEstado("Inactivo");

        when(userClient.getUserById(1L)).thenReturn(usuario);
        when(productClient.getProductById(10L)).thenReturn(producto);

        assertThatThrownBy(() -> orderService.save(solicitud))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("no existe o está inactivo");

        verifyNoInteractions(inventoryClient);
    }

    @Test
    @DisplayName("Rechaza la orden cuando Inventario rechaza la reserva")
    void shouldThrowExceptionWhenInventoryFails() {
        OrderSaveDTO solicitud = solicitudDeOrden();

        when(userClient.getUserById(1L)).thenReturn(usuario);
        when(productClient.getProductById(10L)).thenReturn(producto);
        when(inventoryClient.reservarStock(eq(10L), any(InventoryCantidadDTO.class)))
                .thenThrow(mock(FeignException.class));

        assertThatThrownBy(() -> orderService.save(solicitud))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("No hay stock suficiente para el producto");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Actualiza el estado usando la consulta con bloqueo")
    void shouldUpdateEstado() {
        when(orderRepository.findOrdenById(100L)).thenReturn(Optional.of(ordenPrueba));
        when(orderRepository.save(ordenPrueba)).thenReturn(ordenPrueba);

        OrderDetalleDTO resultado = orderService.updateEstado(100L, cambioA("PAGADA"));

        assertThat(resultado.getEstado()).isEqualTo("PAGADA");
        verify(orderRepository).findOrdenById(100L);
        verify(orderRepository).save(ordenPrueba);
    }

    @Test
    @DisplayName("Repetir una aprobación ya guardada conserva la orden pagada")
    void shouldIgnoreRepeatedPaymentApproval() {
        ordenPrueba.setEstado("PAGADA");
        when(orderRepository.findOrdenById(100L)).thenReturn(Optional.of(ordenPrueba));

        OrderDetalleDTO resultado = orderService.updateEstado(100L, cambioA("PAGADA"));

        assertThat(resultado.getEstado()).isEqualTo("PAGADA");
        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(inventoryClient);
    }

    @Test
    @DisplayName("Repetir la transición a cancelada no vuelve a liberar stock")
    void shouldIgnoreRepeatedCanceledState() {
        ordenPrueba.setEstado("CANCELADA");
        when(orderRepository.findOrdenById(100L)).thenReturn(Optional.of(ordenPrueba));

        OrderDetalleDTO resultado = orderService.updateEstado(100L, cambioA("CANCELADA"));

        assertThat(resultado.getEstado()).isEqualTo("CANCELADA");
        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(inventoryClient);
    }

    @Test
    @DisplayName("Cancela una orden pendiente y libera su reserva")
    void shouldCancelarOrden() {
        when(orderRepository.findOrdenById(100L)).thenReturn(Optional.of(ordenPrueba));
        when(inventoryClient.liberarStock(eq(10L), any(InventoryCantidadDTO.class)))
                .thenReturn(inventario);

        orderService.cancelarOrden(100L);

        assertThat(ordenPrueba.getEstado()).isEqualTo("CANCELADA");
        verify(inventoryClient).liberarStock(
                eq(10L),
                argThat(dto -> Long.valueOf(2L).equals(dto.getCantidad()))
        );
        verify(orderRepository).save(ordenPrueba);
    }

    @Test
    @DisplayName("Rechaza una segunda cancelación manual sin modificar inventario")
    void shouldThrowExceptionWhenOrderIsAlreadyCanceled() {
        ordenPrueba.setEstado("CANCELADA");
        when(orderRepository.findOrdenById(100L)).thenReturn(Optional.of(ordenPrueba));

        assertThatThrownBy(() -> orderService.cancelarOrden(100L))
                .isInstanceOf(OrderException.class)
                .hasMessage("La orden ya se encuentra cancelada.");

        verifyNoInteractions(inventoryClient);
        verify(orderRepository, never()).save(any(Order.class));
    }

    private OrderSaveDTO solicitudDeOrden() {
        OrderSaveItemDTO item = new OrderSaveItemDTO();
        item.setProductId(10L);
        item.setCantidad(2L);

        OrderSaveDTO solicitud = new OrderSaveDTO();
        solicitud.setUserId(1L);
        solicitud.setItems(List.of(item));
        return solicitud;
    }

    private OrderUpdateEstadoDTO cambioA(String estado) {
        OrderUpdateEstadoDTO cambio = new OrderUpdateEstadoDTO();
        cambio.setEstado(estado);
        return cambio;
    }
}