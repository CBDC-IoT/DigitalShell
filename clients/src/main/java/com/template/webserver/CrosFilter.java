package com.template.webserver;
import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

//public class CrosFilter implements javax.servlet.Filter {
//    @Override
//    public void init(FilterConfig filterConfig) throws ServletException {
//
//    }
//
//    @Override
//    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
//        HttpServletResponse res = (HttpServletResponse) servletResponse;
//        //*号表示对所有请求都允许跨域访问
//        res.addHeader("Access-Control-Allow-Origin", "http://localhost:8080");
//        res.addHeader("Access-Control-Allow-Methods", "GET");
//        filterChain.doFilter(servletRequest, servletResponse);
//    }
//
//    @Override
//    public void destroy() {
//
//    }
//}
