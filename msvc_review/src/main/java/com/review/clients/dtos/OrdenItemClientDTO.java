package com.review.clients.dtos;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class OrdenItemClientDTO {
    private Long productId;
    private Integer cantidad;
}
