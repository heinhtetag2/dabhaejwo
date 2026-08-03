package com.dabhaejwo.global.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/** {@link AuthPrincipal} 을 SecurityContext 에 싣기 위한 얇은 래퍼. */
public class PrincipalAuthentication extends AbstractAuthenticationToken {

    private final AuthPrincipal principal;

    private PrincipalAuthentication(AuthPrincipal principal, List<GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        setAuthenticated(true);
    }

    public static PrincipalAuthentication of(AuthPrincipal principal) {
        List<GrantedAuthority> authorities = switch (principal) {
            case AuthPrincipal.Operator operator -> operator.role().permissions().stream()
                    .map(p -> (GrantedAuthority) new SimpleGrantedAuthority(p.name()))
                    .toList();
            case AuthPrincipal.TenantUser user ->
                    List.of(new SimpleGrantedAuthority("TENANT_" + user.role().name()));
            case AuthPrincipal.Visitor ignored ->
                    List.of(new SimpleGrantedAuthority("VISITOR"));
        };
        return new PrincipalAuthentication(principal, authorities);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public AuthPrincipal getPrincipal() {
        return principal;
    }
}
