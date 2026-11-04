package com.flashticket.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * JWT Access Token 服务（RS256）。
 *
 * <p>令牌内容（见 doc/用户与账号模块设计.md 四）：
 * <ul>
 *   <li>sub = 用户 ID（字符串，前端按字符串处理雪花主键）</li>
 *   <li>username / roles / perms / sid(sessionId) 自定义声明</li>
 *   <li>iat / exp，有效期 15 分钟</li>
 * </ul>
 *
 * <p>私钥仅用于签发，公钥用于校验；两者来自 classpath PEM（开发用密钥，
 * 生产由外部密钥管理系统注入，见 application.yml 注释）。
 */
@Component
public class JwtService {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final Duration accessTokenTtl;

    public JwtService(@Value("${flashticket.jwt.private-key}") Resource privateKeyResource,
                      @Value("${flashticket.jwt.public-key}") Resource publicKeyResource,
                      @Value("${flashticket.jwt.access-token-ttl}") Duration accessTokenTtl) {
        this.privateKey = readPrivateKey(privateKeyResource);
        this.publicKey = readPublicKey(publicKeyResource);
        this.accessTokenTtl = accessTokenTtl;
    }

    /** 签发 Access Token。 */
    public String generateAccessToken(Long userId, String username, List<String> roles,
                                      List<String> perms, String sessionId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("roles", roles)
                .claim("perms", perms)
                .claim("sid", sessionId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /** 解析并校验签名/有效期；无效则抛 {@link JwtException}。 */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    /* ---------- 密钥解析 ---------- */

    private PrivateKey readPrivateKey(Resource resource) {
        try {
            byte[] der = decodePem(resource, "PRIVATE KEY");
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception ex) {
            throw new IllegalStateException("无法加载 JWT 私钥：" + resource, ex);
        }
    }

    private PublicKey readPublicKey(Resource resource) {
        try {
            byte[] der = decodePem(resource, "PUBLIC KEY");
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception ex) {
            throw new IllegalStateException("无法加载 JWT 公钥：" + resource, ex);
        }
    }

    private byte[] decodePem(Resource resource, String label) throws IOException {
        String pem = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String base64 = pem
                .replace("-----BEGIN " + label + "-----", "")
                .replace("-----END " + label + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}
