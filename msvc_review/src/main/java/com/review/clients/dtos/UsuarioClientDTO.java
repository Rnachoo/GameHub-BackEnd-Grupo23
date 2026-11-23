package com.review.clients.dtos;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class UsuarioClientDTO {
    private Long id;
    private String nombreUser;
    private String email;
    private String estado;
}
