package com.ems.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

/**
 * JWT令牌提供者
 *
 * @author EMS Team
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret:ems-mqtt-device-management-system-secret-key-2024}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24小时
    private long jwtExpirationInMs;

    @Value("${jwt.refresh-expiration:604800000}") // 7天
    private long refreshExpirationInMs;

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 从令牌中提取用户名
     */
    public String getUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 从令牌中提取过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 从令牌中提取企业ID
     */
    public Long getEnterpriseIdFromToken(String token) {
        return extractClaim(token, claims -> claims.get("enterpriseId", Long.class));
    }

    /**
     * 从令牌中提取用户角色
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        return extractClaim(token, claims -> (List<String>) claims.get("roles"));
    }

    /**
     * 从令牌中提取指定声明
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 从令牌中提取所有声明
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.error("JWT token is malformed: {}", e.getMessage());
            throw e;
        } catch (SecurityException e) {
            log.error("JWT signature validation failed: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("JWT token compact of handler are invalid: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 检查令牌是否过期
     */
    public Boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * 生成访问令牌
     */
    public String generateAccessToken(UserDetails userDetails, Long enterpriseId) {
        return createToken(userDetails, enterpriseId, jwtExpirationInMs);
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(UserDetails userDetails, Long enterpriseId) {
        return createToken(userDetails, enterpriseId, refreshExpirationInMs);
    }

    /**
     * 创建令牌
     */
    private String createToken(UserDetails userDetails, Long enterpriseId, long expiration) {
        Instant now = Instant.now();
        Instant expiryDate = now.plus(expiration, ChronoUnit.MILLIS);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .claim("enterpriseId", enterpriseId)
                .claim("roles", userDetails.getAuthorities().stream()
                        .map(auth -> auth.getAuthority())
                        .toList())
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 验证令牌
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = getUsernameFromToken(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证令牌格式
     */
    public Boolean validateTokenFormat(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Token format validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取令牌剩余有效时间（毫秒）
     */
    public long getTokenRemainingValidity(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 检查是否需要刷新令牌（剩余时间小于30分钟）
     */
    public Boolean shouldRefreshToken(String token) {
        long remainingValidity = getTokenRemainingValidity(token);
        return remainingValidity > 0 && remainingValidity < (30 * 60 * 1000); // 30分钟
    }
}