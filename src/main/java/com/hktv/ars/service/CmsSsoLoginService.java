package com.hktv.ars.service;


import com.hktv.ars.data.sso.AuthCmsRequestData;
import com.hktv.ars.data.sso.AuthResponseData;
import com.hktv.ars.data.sso.CmsLoginMainRequestData;
import com.hktv.ars.data.sso.CmsLoginMainResponseData;

public interface CmsSsoLoginService {
    CmsLoginMainResponseData login(CmsLoginMainRequestData requestData);
    AuthResponseData renewCmsToken(AuthCmsRequestData authenticationRequest);
}
