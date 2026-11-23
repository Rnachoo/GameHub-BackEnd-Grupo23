package com.shipping.clients.dtos;

import lombok.*;
import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class UsuarioClientDTO {
    private Long id;
    private String nombreUser;
    private String estado;
    private String email;
    private List<DireccionClientDTO> directionsDTO;
}