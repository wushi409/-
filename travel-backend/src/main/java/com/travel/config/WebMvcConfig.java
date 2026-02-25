/**
 * MVC 配置类。
 * 主要注册拦截器并统一处理接口权限校验。
 */
package com.travel.config;

import com.travel.common.Result;
import com.travel.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
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
            // 放行OPTIONS请求
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                return true;
            }

            // 非Controller方法直接放行
            if (!(handler instanceof HandlerMethod)) {
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

                // 检查角色权限
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
