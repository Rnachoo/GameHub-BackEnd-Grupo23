package com.review.services;

import com.review.models.dtos.ResenaRequestDTO;
import com.review.models.dtos.ResenaResponseDTO;
import com.review.models.dtos.ResenaUpdateDTO;

import java.util.List;

public interface ReviewService {
    ResenaResponseDTO createResena(ResenaRequestDTO requestDTO);
    ResenaResponseDTO getResenaById(Long id);
    List<ResenaResponseDTO> getResenasByProductoId(Long productoId);
    List<ResenaResponseDTO> getResenasByUsuarioId(Long usuarioId);
    ResenaResponseDTO updateResena(Long id, ResenaUpdateDTO requestDTO);
    ResenaResponseDTO moderarResena(Long id);
    void validarDueno(Long usuarioId, String email);
    void validarDuenoResena(Long id, String email);
    List<ResenaResponseDTO> getResenasParaModerar(Long productoId);
}