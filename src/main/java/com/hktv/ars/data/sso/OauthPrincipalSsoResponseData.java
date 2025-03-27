package com.hktv.ars.data.sso;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OauthPrincipalSsoResponseData {

    private String username;

    List<AuthoritiesSsoResponseData> authorities;

    private Boolean enabled;
}
