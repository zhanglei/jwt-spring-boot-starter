package cn.graydove.security.filter;


import cn.graydove.security.exception.DenyException;
import cn.graydove.security.exception.InvalidTokenException;
import cn.graydove.security.exception.UnauthorizedException;
import cn.graydove.security.handler.DenyHandler;
import cn.graydove.security.handler.UnauthorizedHandler;
import cn.graydove.security.properties.JwtProperties;
import cn.graydove.security.token.TokenManager;
import cn.graydove.security.token.authority.AuthorityMatcher;
import cn.graydove.security.userdetails.UserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class TokenFilter extends BaseFilter {

    private ObjectMapper objectMapper;

    private JwtProperties jwtProperties;

    private TokenManager tokenManager;

    private AuthorityMatcher authorityMatcher;

    private DenyHandler denyHandler;

    private UnauthorizedHandler unauthorizedHandler;

    private Class<? extends UserDetails> userClass;

    public TokenFilter(ObjectMapper objectMapper, JwtProperties jwtProperties, TokenManager tokenManager, AuthorityMatcher authorityMatcher, DenyHandler denyHandler, UnauthorizedHandler unauthorizedHandler, Class<? extends UserDetails> userClass) {
        this.objectMapper = objectMapper;
        this.jwtProperties = jwtProperties;
        this.tokenManager = tokenManager;
        this.authorityMatcher = authorityMatcher;
        this.denyHandler = denyHandler;
        this.unauthorizedHandler = unauthorizedHandler;
        this.userClass = userClass;
    }

    @Override
    public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        String requestURI = request.getRequestURI();

        String header = request.getHeader(jwtProperties.getToken().getHeader());
        UserDetails user;
        try {
            user = check(header);
            request.setAttribute(UserDetails.USER_PARAM_NAME, user);

            if (!authorityMatcher.match(requestURI).asserts(user)) {
                throw new DenyException();
            }
        } catch (InvalidTokenException | UnauthorizedException e) {
            if (!authorityMatcher.match(requestURI).asserts(null)) {
                unauthorizedHandler.handle(request, response, e);
                return;
            }
        } catch (DenyException e) {
            denyHandler.handle(request, response, e);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private UserDetails check(String header) throws InvalidTokenException, UnauthorizedException {
        if (header == null) {
            throw new UnauthorizedException();
        }
        String prefix = jwtProperties.getToken().getPrefix();
        if (!header.startsWith(prefix)) {
            throw new InvalidTokenException();
        }
        String jwt = header.substring(prefix.length() + 1);
        Claims claims;
        try {
            claims = tokenManager.parseJWT(jwt);
        } catch (Exception e) {
            throw new InvalidTokenException();
        }
        if (jwtProperties.getToken().getIssuer().equals(claims.getIssuer())) {
            try {
                return objectMapper.readValue(claims.getSubject(), userClass);
            } catch (Exception e) {
                log.warn("user序列化失败：" + e.getMessage(), e);
                throw new InvalidTokenException();
            }
        }
        throw new InvalidTokenException();
    }


    @Override
    public String getName() {
        return "TOKEN_FILTER";
    }

}