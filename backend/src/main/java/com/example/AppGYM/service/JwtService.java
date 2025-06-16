// backend/src/main/java/com/example/AppGYM/service/JwtService.java
package com.example.AppGYM.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.secret}")          // define JWT_SECRET en Railway
    private String secret;

    private SecretKey key() { return Keys.hmacShaKeyFor(secret.getBytes()); }

    /* -------- creación -------- */
    public String generateToken(String username){
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+1000L*60*60*24))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    /* -------- lectura -------- */
    public Claims extractAllClaims(String token){
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    public <T> T extractClaim(String token, Function<Claims,T> map){
        return map.apply(extractAllClaims(token));
    }

    /* -------- validación -------- */
    public boolean validateToken(String token, UserDetails ud){
        String user = extractClaim(token, Claims::getSubject);
        boolean expired = extractClaim(token, Claims::getExpiration).before(new Date());
        return user.equals(ud.getUsername()) && !expired;
    }
}
