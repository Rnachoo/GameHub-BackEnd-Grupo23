package com.product.services;

import com.product.clients.CategoryFeignClient;
import com.product.clients.dtos.CategoryClientDTO;
import com.product.exceptions.ProductException;
import com.product.exceptions.RecursoNoEncontradoException;
import com.product.models.Product;
import com.product.models.dtos.ProductRequestDTO;
import com.product.models.dtos.ProductResponseDTO;
import com.product.repositories.ProductRepository;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryFeignClient categoryFeignClient;

    @Transactional
    @Override
    public ProductResponseDTO createProduct(ProductRequestDTO requestDTO) {
        validarCategoria(requestDTO.getCategoriaId());
        validarPrecioDescuento(requestDTO);

        Product product = new Product();
        copiarDatos(requestDTO, product);
        product.setEstado("Activo");

        Product saved = productRepository.save(product);
        log.info("Producto creado con ID: {}", saved.getId());
        return toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    @Override
    public ProductResponseDTO getProductById(Long id) {
        return toResponseDTO(buscar(id));
    }

    @Transactional(readOnly = true)
    @Override
    public List<ProductResponseDTO> getAllProducts() {
        List<Product> products = productRepository.findAll();
        log.info("Total de productos encontrados: {}", products.size());
        return products.stream().map(this::toResponseDTO).toList();
    }

    @Transactional
    @Override
    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO requestDTO) {
        validarCategoria(requestDTO.getCategoriaId());
        validarPrecioDescuento(requestDTO);
        Product product = buscar(id);

        copiarDatos(requestDTO, product);
        Product updated = productRepository.save(product);
        log.info("Producto actualizado con ID: {}", updated.getId());
        return toResponseDTO(updated);
    }

    @Transactional
    @Override
    public void deactivateProduct(Long id) {
        Product product = buscar(id);
        product.setEstado("Inactivo");
        productRepository.save(product);
        log.info("Producto desactivado con ID: {}", id);
    }

    private Product buscar(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado con ID: " + id));
    }

    private void validarCategoria(Long categoriaId) {
        CategoryClientDTO categoria;
        try {
            categoria = categoryFeignClient.getCategoryById(categoriaId);
        } catch (FeignException e) {
            log.warn("No se pudo validar la categoría {}: {}", categoriaId, e.status());
            throw new ProductException("Categoría no encontrada o inactiva.");
        }
        if (categoria == null || !"Active".equals(categoria.getEstado())) {
            throw new ProductException("Categoría no encontrada o inactiva.");
        }
    }

    private void validarPrecioDescuento(ProductRequestDTO dto) {
        if (dto.getPrecioDescuento() != null && dto.getPrecioDescuento() >= dto.getPrecio()) {
            throw new ProductException("El precio con descuento debe ser menor que el precio normal.");
        }
    }

    private void copiarDatos(ProductRequestDTO dto, Product product) {
        product.setNombre(dto.getNombre());
        product.setMarca(dto.getMarca());
        product.setModelo(dto.getModelo());
        product.setPrecio(dto.getPrecio());
        product.setPrecioDescuento(dto.getPrecioDescuento());
        product.setDestacado(Boolean.TRUE.equals(dto.getDestacado()));
        product.setCategoriaId(dto.getCategoriaId());
        product.setDescripcion(dto.getDescripcion());
        product.getImagenes().clear();
        if (dto.getImagenes() != null) {
            product.getImagenes().addAll(dto.getImagenes());
        }
        product.getEspecificaciones().clear();
        if (dto.getEspecificaciones() != null) {
            product.getEspecificaciones().addAll(dto.getEspecificaciones());
        }
    }

    private ProductResponseDTO toResponseDTO(Product product) {
        ProductResponseDTO dto = new ProductResponseDTO();
        dto.setId(product.getId());
        dto.setNombre(product.getNombre());
        dto.setMarca(product.getMarca());
        dto.setModelo(product.getModelo());
        dto.setPrecio(product.getPrecio());
        dto.setPrecioDescuento(product.getPrecioDescuento());
        dto.setDestacado(Boolean.TRUE.equals(product.getDestacado()));
        dto.setCategoriaId(product.getCategoriaId());
        dto.setDescripcion(product.getDescripcion());
        dto.setEstado(product.getEstado());
        dto.setImagenes(new ArrayList<>(product.getImagenes()));
        dto.setEspecificaciones(new ArrayList<>(product.getEspecificaciones()));
        return dto;
    }
}
