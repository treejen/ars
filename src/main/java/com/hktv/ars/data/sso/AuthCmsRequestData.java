package com.hktv.ars.data.sso;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthCmsRequestData {

    @NotEmpty(message = "originToken should not be null or empty.")
    private String originToken;

}
