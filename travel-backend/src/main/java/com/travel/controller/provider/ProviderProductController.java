package com.travel.controller.provider;

import com.travel.common.Constants;
import com.travel.common.Result;
import com.travel.config.RequireRole;
import com.travel.entity.TravelProduct;
import com.travel.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id, HttpServletRequest request) {
        Long providerId = (Long) request.getAttribute("userId");
        productService.deleteByProvider(providerId, id);
        return Result.success("product deleted");
    }
}
