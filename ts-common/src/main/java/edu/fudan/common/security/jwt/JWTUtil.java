package edu.fudan.common.security.jwt;

import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import edu.fudan.common.exception.TokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.extern.slf4j.Slf4j;

/**
 * @author fdse
 */
@Slf4j
public class JWTUtil
{
    private static String secretKey = Base64.getEncoder().encodeToString("secret".getBytes());

    private JWTUtil()
    {
        throw new IllegalStateException("Utility class");
    }

    public static Authentication getJWTAuthentication(ServletRequest request)
    {
        String token = getTokenFromHeader((HttpServletRequest) request);
        if (token != null && validateToken(token)) {

            UserDetails userDetails = new UserDetails()
            {
                @Override
                public Collection<? extends GrantedAuthority> getAuthorities()
                {
                    return getRole(token).stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
                }

                @Override
                public String getPassword()
                {
                    return "";
                }

                @Override
                public String getUsername()
                {
                    return getUserName(token);
                }

                @Override
                public boolean isAccountNonExpired()
                {
                    return true;
                }

                @Override
                public boolean isAccountNonLocked()
                {
                    return true;
                }

                @Override
                public boolean isCredentialsNonExpired()
                {
                    return true;
                }

                @Override
                public boolean isEnabled()
                {
                    return true;
                }
            };
            // send to spring security
            return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
        }
        return null;
    }

    private static String getUserName(String token)
    {
        return getClaims(token).getBody().getSubject();
    }

    private static List<String> getRole(String token)
    {
        Jws<Claims> claimsJws = getClaims(token);
        return (List<String>) (claimsJws.getBody().get("roles", List.class));
    }

    private static String getTokenFromHeader(HttpServletRequest request)
    {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7, bearerToken.length());
        }
        return null;
    }

    private static boolean validateToken(String token)
    {
        try {
            Jws<Claims> claimsJws = getClaims(token);
            return !claimsJws.getBody().getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            log.error("Token expired: {} " + e);
            throw new TokenException("Token expired");
        } catch (UnsupportedJwtException e) {
            log.error("Token format error: {} " + e);
            throw new TokenException("Token format error");
        } catch (MalformedJwtException e) {
            log.error("Token is not properly constructed: {} " + e);
            throw new TokenException("Token is not properly constructed");
        } catch (SignatureException e) {
            log.error("Signature failure: {} " + e);
            throw new TokenException("Signature failure");
        } catch (IllegalArgumentException e) {
            log.error("Illegal parameter exception: {} " + e);
            throw new TokenException("Illegal parameter exception");
        }
    }

    private static Jws<Claims> getClaims(String token)
    {
        return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
    }
}
