package com.cdutetc.ems.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT工具类
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 从token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * 从token中获取企业ID
     */
    public Long getCompanyIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("companyId", Long.class);
    }

    /**
     * 从token中获取用户角色
     */
    public String getRoleFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("role", String.class);
    }

    /**
     * 从token中获取过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * 从token中获取指定声明
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 从token中获取所有声明
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 检查token是否过期
     */
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * 为用户生成token
     */
    public String generateToken(UserDetails userDetails, Long companyId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("companyId", companyId);
        claims.put("role", role);
        return createToken(claims, userDetails.getUsername());
    }

    /**
     * 创建token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 验证token
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = getUsernameFromToken(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证token格式和签名
     */
    public Boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 刷新token
     */
    public String refreshToken(String token) {
        try {
            final Claims claims = getAllClaimsFromToken(token);
            claims.setIssuedAt(new Date());
            claims.setExpiration(new Date(System.currentTimeMillis() + expiration));
            return Jwts.builder()
                    .setClaims(claims)
                    .signWith(getSigningKey())
                    .compact();
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT token refresh failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从token中获取剩余有效时间（毫秒）
     */
    public Long getExpirationTime(String token) {
        try {
            Date expirationDate = getExpirationDateFromToken(token);
            return expirationDate.getTime() - System.currentTimeMillis();
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Failed to get expiration time from token: {}", e.getMessage());
            return 0L;
        }
    }
}