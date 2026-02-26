/**
 * MVC 配置：统一注册 JWT 拦截器与静态资源映射。
 */
package com.travel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.Result;
import com.travel.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/destinations",
                        "/api/destinations/**",
                        "/api/products",
                        "/api/products/**",
                        "/api/upload/**"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/");
    }

    private class JwtInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            // 放行跨域预检请求。
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                return true;
            }

            // 非 Controller 方法（例如静态资源）直接放行。
            if (!(handler instanceof HandlerMethod)) {
                return true;
            }

            // 游记广场列表允许匿名访问；若带 token 则尽量解析，仅用于 onlyMine 场景。
            if ("GET".equalsIgnoreCase(request.getMethod()) && "/api/travel-notes".equals(request.getRequestURI())) {
                String optionalToken = request.getHeader("Authorization");
                if (optionalToken != null && optionalToken.startsWith("Bearer ")) {
                    optionalToken = optionalToken.substring(7);
                }
                if (optionalToken != null && !optionalToken.isEmpty()) {
                    try {
                        Long userId = jwtUtils.getUserIdFromToken(optionalToken);
                        Integer role = jwtUtils.getRoleFromToken(optionalToken);
                        request.setAttribute("userId", userId);
                        request.setAttribute("role", role);
                    } catch (Exception ignored) {
                        // 匿名浏览场景下忽略无效 token，不阻断列表访问。
                    }
                }
                return true;
            }

            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            if (token == null || token.isEmpty()) {
                sendError(response, 401, "未登录");
                return false;
            }

            try {
                Long userId = jwtUtils.getUserIdFromToken(token);
                Integer role = jwtUtils.getRoleFromToken(token);
                request.setAttribute("userId", userId);
                request.setAttribute("role", role);

                // 检查接口角色权限。
                HandlerMethod handlerMethod = (HandlerMethod) handler;
                RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
                if (requireRole == null) {
                    requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
                }
                if (requireRole != null) {
                    boolean hasRole = false;
                    for (int r : requireRole.value()) {
                        if (r == role) {
                            hasRole = true;
                            break;
                        }
                    }
                    if (!hasRole) {
                        sendError(response, 403, "权限不足");
                        return false;
                    }
                }

                return true;
            } catch (Exception e) {
                sendError(response, 401, "登录已过期");
                return false;
            }
        }

        private void sendError(HttpServletResponse response, int code, String message) throws Exception {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(200);
            ObjectMapper mapper = new ObjectMapper();
            response.getWriter().write(mapper.writeValueAsString(Result.error(code, message)));
        }
    }
}
