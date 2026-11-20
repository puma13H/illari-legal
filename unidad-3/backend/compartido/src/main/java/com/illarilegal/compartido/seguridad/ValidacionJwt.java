package com.illarilegal.compartido.seguridad;

import java.time.Duration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** La misma política de emisor, audiencia y vigencia para las API y el Gateway. */
public final class ValidacionJwt {
  private ValidacionJwt() {}

  public static NimbusJwtDecoder decodificadorHttp(String issuer, String jwks, String audience) {
    var decoder =
        NimbusJwtDecoder.withJwkSetUri(jwks).jwsAlgorithm(SignatureAlgorithm.RS256).build();
    decoder.setJwtValidator(validadores(issuer, audience));
    return decoder;
  }

  public static OAuth2TokenValidator<Jwt> validadores(String issuer, String audience) {
    OAuth2TokenValidator<Jwt> claims =
        jwt ->
            jwt.getExpiresAt() != null
                    && jwt.getSubject() != null
                    && !jwt.getSubject().isBlank()
                    && jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(
                        "invalid_token",
                        "Required subject, expiration or audience missing.",
                        null));
    return new DelegatingOAuth2TokenValidator<>(
        new JwtIssuerValidator(issuer), new JwtTimestampValidator(Duration.ofSeconds(5)), claims);
  }
}
