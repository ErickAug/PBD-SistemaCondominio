package br.com.condominio.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtService {

    private final SecretKey chaveAssinatura;
    private final long tempoExpiracaoMillis;

    public JwtService(@Value("${jwt.secret}") String secretConfigurado,
                      @Value("${jwt.expiration-millis}") long tempoExpiracaoMillis) {
        byte[] chaveDecodificada = Base64.getDecoder().decode(secretConfigurado);
        this.chaveAssinatura = Keys.hmacShaKeyFor(chaveDecodificada);
        this.tempoExpiracaoMillis = tempoExpiracaoMillis;
    }

    public String gerarToken(String usuarioLogin) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + tempoExpiracaoMillis);

        return Jwts.builder()
                .subject(usuarioLogin)
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(chaveAssinatura)
                .compact();
    }

    public String extrairUsuarioLogin(String token) {
        return extrairClaim(token, Claims::getSubject);
    }

    public boolean tokenValido(String token, String usuarioLogin) {
        String loginDoToken = extrairUsuarioLogin(token);
        return loginDoToken.equals(usuarioLogin) && !tokenExpirado(token);
    }

    private boolean tokenExpirado(String token) {
        Date expiracao = extrairClaim(token, Claims::getExpiration);
        return expiracao.before(new Date());
    }

    private <T> T extrairClaim(String token, Function<Claims, T> resolvedor) {
        Claims claims = Jwts.parser()
                .verifyWith(chaveAssinatura)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return resolvedor.apply(claims);
    }
}