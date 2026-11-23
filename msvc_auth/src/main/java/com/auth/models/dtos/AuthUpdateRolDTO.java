package com.auth.models.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class AuthUpdateRolDTO {

    @NotBlank(message = "El rol es obligatorio")
    private String rol;

}
