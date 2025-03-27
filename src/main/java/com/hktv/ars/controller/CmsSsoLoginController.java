package com.hktv.ars.controller;


import com.hktv.ars.data.sso.AuthCmsRequestData;
import com.hktv.ars.data.sso.AuthResponseData;
import com.hktv.ars.data.sso.CmsLoginMainRequestData;
import com.hktv.ars.data.sso.CmsLoginMainResponseData;
import com.hktv.ars.service.CmsSsoLoginService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@RequestMapping("cms/sso")
@RestController
public class CmsSsoLoginController {
    private final CmsSsoLoginService cmsSsoLoginService;

    @Operation(summary = "log in", description = "log in", tags = {"CMS sso"})
    @PostMapping("login")
    public CmsLoginMainResponseData login(@RequestBody @Valid CmsLoginMainRequestData requestData) {
        return cmsSsoLoginService.login(requestData);
    }

    @Operation(summary = "renew token", description = "renew token", tags = {"CMS sso"})
    @PostMapping("renew_token")
    public AuthResponseData renewCmsToken(@RequestBody @Valid AuthCmsRequestData authenticationRequest) {
        return cmsSsoLoginService.renewCmsToken(authenticationRequest);
    }
}
