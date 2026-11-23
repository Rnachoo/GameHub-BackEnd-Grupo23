package com.shipping.clients.dtos;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class DireccionClientDTO {
    private String comuna;
    private String ciudad;
    private String calle;
    private String numero;
}
