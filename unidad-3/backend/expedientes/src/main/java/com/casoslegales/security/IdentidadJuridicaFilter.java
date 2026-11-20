package com.casoslegales.security;
import com.casoslegales.repository.IdentidadKeycloakRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

/** Resolve an approved authority/sub binding after JWT issuer validation; emails never select an account. */
public class IdentidadJuridicaFilter extends OncePerRequestFilter {
    private final IdentidadKeycloakRepository identidades;
    private final String authority;
    public IdentidadJuridicaFilter(IdentidadKeycloakRepository identidades,String authority){this.identidades=identidades;this.authority=authority;}
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path=request.getServletPath();
        return !path.startsWith("/api/") || path.equals("/api/identidades/vinculos") || path.startsWith("/api/auth/");
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        if(authentication instanceof JwtAuthenticationToken token) {
            var jwt=token.getToken();
            var binding=identidades.findByIssuerAndSubject(authority,jwt.getSubject());
            if(binding.isEmpty()){response.sendError(403,"Identidad jurídica sin vinculación aprobada.");return;}
            var user=binding.get().getUser();
            String role=user.getRole().getName();
            if(token.getAuthorities().stream().noneMatch(a->a.getAuthority().equals("ROLE_"+role))) {
                response.sendError(403,"El rol jurídico no coincide con el rol autorizado.");return;
            }
            SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
                    List.of(new SimpleGrantedAuthority("ROLE_"+role)),user.getEmail()));
        }
        chain.doFilter(request,response);
    }
}

