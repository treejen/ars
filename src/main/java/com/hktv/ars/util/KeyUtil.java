package com.hktv.ars.util;


import com.hktv.ars.enums.CustomErrorLogMessage;
import com.hktv.ars.exception.CustomRuntimeException;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
@Getter
@Setter
@Slf4j
@RequiredArgsConstructor
public class KeyUtil {

    @Value("${ars.cms-auth.publicKey}")
    private String cmsPublicKey;
    @Value("${ars.cms-auth.privateKey}")
    private String cmsPrivateKey;

    private PrivateKey cmsAuthPrivateKey;
    private PublicKey cmsAuthPublicKey;

    @PostConstruct
    public void initAuthKey() {
        try {
            this.cmsAuthPublicKey = loadcmsAuthPublicKey(cmsPublicKey);
            this.cmsAuthPrivateKey = loadcmsAuthPrivateKey(cmsPrivateKey);
        } catch (Exception e) {
            log.info("Loading key failed, exception: ",e);
            throw new CustomRuntimeException(CustomErrorLogMessage.KEY_LOAD_FAILED);
        }
        log.info("loaded Keys");
    }

    private PrivateKey loadcmsAuthPrivateKey(String cmsAuthPrivateKeyString) throws NoSuchAlgorithmException, InvalidKeySpecException {
        log.info("loading cmsAuthPrivateKey");
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(cmsAuthPrivateKeyString));
        return keyFactory.generatePrivate(keySpecPKCS8);
    }

    private PublicKey loadcmsAuthPublicKey(String cmsAuthPublicKeyString) throws NoSuchAlgorithmException, InvalidKeySpecException {
        log.info("loading cmsAuthPublicKey");
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder().decode(cmsAuthPublicKeyString));
        return keyFactory.generatePublic(keySpecX509);
    }
}
