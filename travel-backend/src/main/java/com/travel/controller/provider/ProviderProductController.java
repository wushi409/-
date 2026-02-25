package com.travel.controller.provider;

import com.travel.common.Constants;
import com.travel.common.Result;
import com.travel.config.RequireRole;
import com.travel.entity.TravelProduct;
import com.travel.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 类说明：ProviderProductController
 * 1. 负责该业务模块的核心流程编排；
 * 2. 通过分层设计保证职责清晰、便于维护；
 * 3. 为上层调用提供稳定、可复用的能力。
 */
@RestController
@RequestMapping("/api/provider/products")
@RequireRole({Constants.ROLE_PROVIDER})
@RequiredArgsConstructor
public class ProviderProductController {

    private final ProductService productService;

    @GetMapping
    public Result<?> list(HttpServletRequest request,
                          @RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) String keyword) {
        Long providerId = (Long) request.getAttribute("userId");
        return Result.success(productService.providerList(providerId, page, size, keyword));
    }

    /**
     * 方法说明：add
     * 1. 负责处理 add 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PostMapping
    public Result<?> add(HttpServletRequest request, @RequestBody TravelProduct product) {
        Long providerId = (Long) request.getAttribute("userId");
        product.setProviderId(providerId);
        productService.add(product);
        return Result.success("product published");
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id,
                            HttpServletRequest request,
                            @RequestBody TravelProduct product) {
        Long providerId = (Long) request.getAttribute("userId");
        product.setId(id);
        productService.updateByProvider(providerId, product);
        return Result.success("product updated");
    }

    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id,
                                  HttpServletRequest request,
                                  @RequestBody java.util.Map<String, Integer> params) {
        Long providerId = (Long) request.getAttribute("userId");
        productService.updateStatusByProvider(providerId, id, params.get("status"));
        return Result.success("status updated");
    }

    /**
     * 方法说明：delete
     * 1. 负责处理 delete 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id, HttpServletRequest request) {
        Long providerId = (Long) request.getAttribute("userId");
        productService.deleteByProvider(providerId, id);
        return Result.success("product deleted");
    }
}
