package com.review.clients.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class OrdenClientDTO {
    private Long id;
    @JsonAlias("userId")
    private Long usuarioId;
    private String estado;
    private List<OrdenItemClientDTO> items = new ArrayList<>();
}
