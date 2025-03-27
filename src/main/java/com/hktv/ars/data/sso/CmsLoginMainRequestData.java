package com.hktv.ars.data.sso;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CmsLoginMainRequestData {

    @NotBlank(message = "User name can't be blank.")
    private String username;

    @NotBlank(message = "Password code can't be blank.")
    private String password;
}
