package com.impetus.api_gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Component
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long accessTokenExp;
    private final long refreshTokenExp;

    public JwtService(
            @Value("${jwt.private-key-path}")Resource privateKeyResource,
            @Value("${jwt.public-key-path}") Resource publicKeyResource,
            @Value("${jwt.access-exp}") long accessTokenExp,
            @Value("${jwt.refresh-exp}") long refreshTokenExp
    ){
        try{
            if(privateKeyResource == null || !privateKeyResource.exists()){
                log.info(privateKeyResource+"  : Pri");
                throw new IllegalStateException("Key Resource not found at path" + privateKeyResource);
            }

            if(publicKeyResource == null || !publicKeyResource.exists()){
                log.info(publicKeyResource+ "  : Pub");
                throw new IllegalStateException("Key Resource not found at path" + publicKeyResource);
            }

            this.privateKey = readPrivateKey(privateKeyResource.getInputStream());
            this.publicKey = readPublicKey(publicKeyResource.getInputStream());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.accessTokenExp = Math.max(0, accessTokenExp);
        this.refreshTokenExp = Math.max(0, refreshTokenExp);
    }

    public Claims extractAllClaims(String token){
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUserName(String token){
        log.info(extractAllClaims(token).getSubject().toString());
        return extractAllClaims(token).getSubject();
    }

    public List<String> extractRoles(String token){
        Claims claims = extractAllClaims(token);
        Object rolesObj = claims.get("roles");

        if(rolesObj instanceof List<?> list){
            return list.stream()
                    .map(Object::toString)
                    .toList();
        }
        return  List.of();
    }

    public boolean isTokenValid(String token){
        try {
            Claims claims = extractAllClaims(token);
            log.info("Claims extracted: "+claims.toString());
            Date exp = claims.getExpiration();
            log.info("Token expiration: "+exp.toString());
            return exp == null || exp.after(new Date());
        }catch (JwtException | IllegalArgumentException e){
            log.info("Token is invalid: " + e.getMessage());
            return false;
        }
    }


    private static PublicKey readPublicKey(InputStream is) throws Exception {
        byte[] rawBytes = is.readAllBytes();

        // Detect encoding
        String encoding;
        if (rawBytes.length >= 2 && rawBytes[0] == (byte) 0xFF && rawBytes[1] == (byte) 0xFE) {
            encoding = "UTF-16LE";
        } else if (rawBytes.length >= 2 && rawBytes[0] == (byte) 0xFE && rawBytes[1] == (byte) 0xFF) {
            encoding = "UTF-16BE";
        } else if (rawBytes.length >= 3 && rawBytes[0] == (byte) 0xEF && rawBytes[1] == (byte) 0xBB && rawBytes[2] == (byte) 0xBF) {
            encoding = "UTF-8-BOM";
        } else {
            encoding = "UTF-8";
        }

        System.out.println("Detected encoding: " + encoding);

        // Decode based on detected encoding
        String pem;
        switch (encoding) {
            case "UTF-16LE":
                pem = new String(rawBytes, StandardCharsets.UTF_16LE);
                break;
            case "UTF-16BE":
                pem = new String(rawBytes, StandardCharsets.UTF_16BE);
                break;
            default:
                pem = new String(rawBytes, StandardCharsets.UTF_8);
        }

        // Normalize PEM
        pem = pem.replace("\uFEFF", "").replace("\r", "").replace("\n", "").trim();

        if (pem.contains("-----BEGIN PUBLIC KEY-----")) {
            String base64 = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] der = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));

        } else if (pem.contains("-----BEGIN RSA PUBLIC KEY-----")) {
            String base64 = pem.replace("-----BEGIN RSA PUBLIC KEY-----", "")
                    .replace("-----END RSA PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] pkcs1Bytes = Base64.getDecoder().decode(base64);

            org.bouncycastle.asn1.pkcs.RSAPublicKey rsaPublicKey =
                    org.bouncycastle.asn1.pkcs.RSAPublicKey.getInstance(pkcs1Bytes);

            org.bouncycastle.asn1.x509.SubjectPublicKeyInfo spki =
                    new org.bouncycastle.asn1.x509.SubjectPublicKeyInfo(
                            new org.bouncycastle.asn1.x509.AlgorithmIdentifier(
                                    org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.rsaEncryption),
                            rsaPublicKey);

            System.out.println("Public key read successfully : "+KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(spki.getEncoded())).toString());
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(spki.getEncoded()));
        }

        throw new IllegalArgumentException("Unsupported public key format. Content starts with: " +
                pem.substring(0, Math.min(50, pem.length())));
    }

    private static PrivateKey readPrivateKey(InputStream is) throws Exception {
        String pem = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();

        // Normalize PEM content
        pem = pem.replace("\r", "").replace("\n", "");

        if (pem.contains("-----BEGIN PRIVATE KEY-----")) {
            // Already PKCS#8
            String base64 = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] der = Base64.getDecoder().decode(base64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);

        } else if (pem.contains("-----BEGIN RSA PRIVATE KEY-----")) {
            // PKCS#1 → Convert to PKCS#8 using BouncyCastle
            String base64 = pem.replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] pkcs1Bytes = Base64.getDecoder().decode(base64);

            // Parse PKCS#1 structure
            org.bouncycastle.asn1.pkcs.RSAPrivateKey rsaPrivateKey =
                    org.bouncycastle.asn1.pkcs.RSAPrivateKey.getInstance(pkcs1Bytes);

            // Wrap into PKCS#8
            org.bouncycastle.asn1.pkcs.PrivateKeyInfo pkcs8Info =
                    new org.bouncycastle.asn1.pkcs.PrivateKeyInfo(
                            new org.bouncycastle.asn1.x509.AlgorithmIdentifier(
                                    org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.rsaEncryption),
                            rsaPrivateKey);
            byte[] pkcs8Bytes = pkcs8Info.getEncoded();

            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pkcs8Bytes);
            System.out.println("Private Key Read Successfull");
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        }

        throw new IllegalArgumentException("Unsupported key format. Expected PKCS#8 or PKCS#1 PEM.");
    }
}
