package com.yufu.idaas.sdk.token;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.yufu.idaas.sdk.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import com.yufu.idaas.sdk.bouncycastle.openssl.*;
import com.yufu.idaas.sdk.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import com.yufu.idaas.sdk.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import com.yufu.idaas.sdk.exception.GenerateException;
import com.yufu.idaas.sdk.exception.YufuInitException;
import org.apache.commons.io.Charsets;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.interfaces.RSAPrivateKey;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static com.yufu.idaas.sdk.constants.YufuTokenConstants.*;

/**
 * Created by shuowang on 2018/5/2.
 */
public class RSATokenGenerator implements ITokenGenerator {
    private RSASSASigner signer;
    private String issuer;
    private String tenant;
    private String keyId;

    public RSATokenGenerator(
        String privateKeyPath,
        String password,
        String issuer,
        String tenant,
        String keyId
    ) throws YufuInitException {
        try {
            this.issuer = issuer;
            this.tenant = tenant;
            this.keyId = keyId;
            if (privateKeyPath == null) {
                throw new YufuInitException("key filename cannot be blank");
            }
            PEMParser reader = new PEMParser(new InputStreamReader(
                new FileInputStream(privateKeyPath),
                Charsets.UTF_8
            ));
            Object object = reader.readObject();
            final PrivateKeyInfo keyInfo;
            if (object instanceof PrivateKeyInfo) {
                keyInfo = (PrivateKeyInfo) object;
            } else if (object instanceof PEMEncryptedKeyPair) {
                if (password == null) {
                    throw new YufuInitException("password is required");
                }
                PEMDecryptorProvider provider = new JcePEMDecryptorProviderBuilder().build(password.toCharArray());
                keyInfo = ((PEMEncryptedKeyPair) object).decryptKeyPair(provider).getPrivateKeyInfo();
            } else {
                keyInfo = ((PEMKeyPair) object).getPrivateKeyInfo();
            }
            RSAPrivateKey privateKey = (RSAPrivateKey) (new JcaPEMKeyConverter()).getPrivateKey(keyInfo);
            this.signer = new RSASSASigner(privateKey);
        } catch (FileNotFoundException e) {
            throw new YufuInitException("Can not find private key file in given path " + e.getMessage());
        } catch (PEMException e) {
            throw new YufuInitException("Private key file is not valid: " + e.getMessage());
        } catch (IOException e) {
            throw new YufuInitException("Private key file can not be read: " + e.getMessage());
        }
    }

    public String generate(Map<String, Object> payload) throws GenerateException {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.parse("RS256"))
            .keyID(this.keyId)
            .type(JOSEObjectType.JWT)
            .build();
        String audience = payload.get("aud") != null ? (String) payload.get("aud") : AUDIENCE_YUFU;
        long now = System.currentTimeMillis();
        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
            .audience(audience)
            .expirationTime(new Date(now + TOKEN_EXPIRE_TIME_IN_MS))
            .issueTime(new Date(now))
            .issuer(this.issuer)
            .subject((String) payload.get("sub"));
        Set<String> claimRegisteredNames = JWTClaimsSet.getRegisteredNames();
        Set<String> headerRegisteredNames =JWSHeader.getRegisteredParameterNames();
        for (Map.Entry<String, Object> claim : payload.entrySet()) {
            if (!claimRegisteredNames.contains(claim.getKey()) &&
                !headerRegisteredNames.contains(claim.getKey())) {
                claimsBuilder.claim(claim.getKey(), claim.getValue());
            }
        }
        if (this.tenant != null) {
            claimsBuilder.claim(TENANT_ID_KEY, this.tenant);
        }
        SignedJWT signedJWT = new SignedJWT(header, claimsBuilder.build());
        try {
            signedJWT.sign(signer);
        } catch (JOSEException e) {
            throw new GenerateException(e.getMessage());
        }
        return signedJWT.serialize();
    }

}