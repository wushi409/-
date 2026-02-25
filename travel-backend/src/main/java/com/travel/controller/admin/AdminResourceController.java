package com.travel.controller.admin;

import com.travel.common.Constants;
import com.travel.common.Result;
import com.travel.config.RequireRole;
import com.travel.entity.Attraction;
import com.travel.entity.Destination;
import com.travel.entity.Hotel;
import com.travel.entity.Transport;
import com.travel.entity.TravelProduct;
import com.travel.service.AttractionService;
import com.travel.service.DestinationService;
import com.travel.service.HotelService;
import com.travel.service.ProductService;
import com.travel.service.TransportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 类说明：AdminResourceController
 * 1. 负责该业务模块的核心流程编排；
 * 2. 通过分层设计保证职责清晰、便于维护；
 * 3. 为上层调用提供稳定、可复用的能力。
 */
@RestController
@RequestMapping("/api/admin")
@RequireRole({Constants.ROLE_ADMIN})
@RequiredArgsConstructor
public class AdminResourceController {

    private final DestinationService destinationService;
    private final AttractionService attractionService;
    private final HotelService hotelService;
    private final TransportService transportService;
    private final ProductService productService;

    // ===== destinations =====
    @GetMapping("/destinations")
    public Result<?> listDestinations(@RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "10") Integer size,
                                      @RequestParam(required = false) String keyword) {
        return Result.success(destinationService.adminList(page, size, keyword));
    }

    /**
     * 方法说明：addDestination
     * 1. 负责处理 addDestination 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PostMapping("/destinations")
    public Result<?> addDestination(@RequestBody Destination destination) {
        destinationService.add(destination);
        return Result.success("destination created");
    }

    /**
     * 方法说明：updateDestination
     * 1. 负责处理 updateDestination 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PutMapping("/destinations/{id}")
    public Result<?> updateDestination(@PathVariable Long id, @RequestBody Destination destination) {
        destination.setId(id);
        destinationService.update(destination);
        return Result.success("destination updated");
    }

    /**
     * 方法说明：deleteDestination
     * 1. 负责处理 deleteDestination 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @DeleteMapping("/destinations/{id}")
    public Result<?> deleteDestination(@PathVariable Long id) {
        destinationService.delete(id);
        return Result.success("destination deleted");
    }

    // ===== attractions =====
    @GetMapping("/attractions")
    public Result<?> listAttractions(@RequestParam(defaultValue = "1") Integer page,
                                     @RequestParam(defaultValue = "10") Integer size,
                                     @RequestParam(required = false) Long destinationId,
                                     @RequestParam(required = false) String keyword) {
        return Result.success(attractionService.list(page, size, destinationId, keyword));
    }

    /**
     * 方法说明：addAttraction
     * 1. 负责处理 addAttraction 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PostMapping("/attractions")
    public Result<?> addAttraction(@RequestBody Attraction attraction) {
        attractionService.add(attraction);
        return Result.success("attraction created");
    }

    /**
     * 方法说明：updateAttraction
     * 1. 负责处理 updateAttraction 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PutMapping("/attractions/{id}")
    public Result<?> updateAttraction(@PathVariable Long id, @RequestBody Attraction attraction) {
        attraction.setId(id);
        attractionService.update(attraction);
        return Result.success("attraction updated");
    }

    /**
     * 方法说明：deleteAttraction
     * 1. 负责处理 deleteAttraction 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @DeleteMapping("/attractions/{id}")
    public Result<?> deleteAttraction(@PathVariable Long id) {
        attractionService.delete(id);
        return Result.success("attraction deleted");
    }

    // ===== hotels =====
    @GetMapping("/hotels")
    public Result<?> listHotels(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size,
                                @RequestParam(required = false) Long destinationId,
                                @RequestParam(required = false) String keyword) {
        return Result.success(hotelService.list(page, size, destinationId, keyword));
    }

    /**
     * 方法说明：addHotel
     * 1. 负责处理 addHotel 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PostMapping("/hotels")
    public Result<?> addHotel(@RequestBody Hotel hotel) {
        hotelService.add(hotel);
        return Result.success("hotel created");
    }

    /**
     * 方法说明：updateHotel
     * 1. 负责处理 updateHotel 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PutMapping("/hotels/{id}")
    public Result<?> updateHotel(@PathVariable Long id, @RequestBody Hotel hotel) {
        hotel.setId(id);
        hotelService.update(hotel);
        return Result.success("hotel updated");
    }

    /**
     * 方法说明：deleteHotel
     * 1. 负责处理 deleteHotel 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @DeleteMapping("/hotels/{id}")
    public Result<?> deleteHotel(@PathVariable Long id) {
        hotelService.delete(id);
        return Result.success("hotel deleted");
    }

    // ===== transports =====
    @GetMapping("/transports")
    public Result<?> listTransports(@RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "10") Integer size,
                                    @RequestParam(required = false) Integer type,
                                    @RequestParam(required = false) String departure,
                                    @RequestParam(required = false) String arrival) {
        return Result.success(transportService.list(page, size, type, departure, arrival));
    }

    /**
     * 方法说明：addTransport
     * 1. 负责处理 addTransport 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PostMapping("/transports")
    public Result<?> addTransport(@RequestBody Transport transport) {
        transport.setStatus(1);
        transportService.add(transport);
        return Result.success("transport created");
    }

    /**
     * 方法说明：updateTransport
     * 1. 负责处理 updateTransport 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PutMapping("/transports/{id}")
    public Result<?> updateTransport(@PathVariable Long id, @RequestBody Transport transport) {
        transport.setId(id);
        transportService.update(transport);
        return Result.success("transport updated");
    }

    /**
     * 方法说明：deleteTransport
     * 1. 负责处理 deleteTransport 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @DeleteMapping("/transports/{id}")
    public Result<?> deleteTransport(@PathVariable Long id) {
        transportService.delete(id);
        return Result.success("transport deleted");
    }

    // ===== products/routes =====
    @GetMapping("/products")
    public Result<?> listProducts(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer size,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) Long destinationId,
                                  @RequestParam(required = false) Long providerId,
                                  @RequestParam(required = false) Integer status) {
        return Result.success(productService.adminList(page, size, keyword, destinationId, providerId, status));
    }

    /**
     * 方法说明：addProduct
     * 1. 负责处理 addProduct 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PostMapping("/products")
    public Result<?> addProduct(@RequestBody TravelProduct product) {
        productService.add(product);
        return Result.success("product created");
    }

    /**
     * 方法说明：updateProduct
     * 1. 负责处理 updateProduct 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PutMapping("/products/{id}")
    public Result<?> updateProduct(@PathVariable Long id, @RequestBody TravelProduct product) {
        product.setId(id);
        productService.update(product);
        return Result.success("product updated");
    }

    /**
     * 方法说明：updateProductStatus
     * 1. 负责处理 updateProductStatus 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PutMapping("/products/{id}/status")
    public Result<?> updateProductStatus(@PathVariable Long id, @RequestBody Map<String, Integer> params) {
        productService.updateStatus(id, params.get("status"));
        return Result.success("product status updated");
    }

    /**
     * 方法说明：deleteProduct
     * 1. 负责处理 deleteProduct 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @DeleteMapping("/products/{id}")
    public Result<?> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return Result.success("product deleted");
    }
}
