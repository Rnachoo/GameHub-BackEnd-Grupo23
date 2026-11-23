package com.shipping.clients.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class OrdenClientDTO {
    private Long id;
    @JsonAlias("userId")
    private Long usuarioId;
    private String estado;
}
