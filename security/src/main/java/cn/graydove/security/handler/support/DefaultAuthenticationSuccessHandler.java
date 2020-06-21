package cn.graydove.security.handler.support;

import cn.graydove.security.handler.AuthenticationSuccessHandler;
import cn.graydove.security.token.Token;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DefaultAuthenticationSuccessHandler implements AuthenticationSuccessHandler {


    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Token token) throws IOException, ServletException {
        response.setCharacterEncoding("utf-8");
        response.getWriter().write(token.toString());
    }
}
