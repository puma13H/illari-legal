package com.illarilegal.gateway;

import com.illarilegal.compartido.seguridad.ValidacionJwt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;

/** The public issuer and the internal JWKS URL serve different purposes. Both are validated. */
@Configuration
public class JwtValidationConfig {
  @Bean
  @ConditionalOnMissingBean(ReactiveJwtDecoder.class)
  public ReactiveJwtDecoder jwtDecoder(
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
      @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwks,
      @Value("${illari.security.audience:illari-api}") String audience) {
    var decoder =
        NimbusReactiveJwtDecoder.withJwkSetUri(jwks)
            .jwsAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
            .build();
    decoder.setJwtValidator(ValidacionJwt.validadores(issuer, audience));
    return decoder;
  }
}
