package com.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.PageResult;
import com.travel.common.exception.BusinessException;
import com.travel.entity.Attraction;
import com.travel.entity.Destination;
import com.travel.entity.Hotel;
import com.travel.entity.Transport;
import com.travel.entity.TravelDayPlan;
import com.travel.entity.TravelProduct;
import com.travel.mapper.AttractionMapper;
import com.travel.mapper.DestinationMapper;
import com.travel.mapper.HotelMapper;
import com.travel.mapper.TransportMapper;
import com.travel.mapper.TravelDayPlanMapper;
import com.travel.mapper.TravelProductMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRouteService {

    private static final String USER_ROUTE_TAG = "USER_ROUTE";

    private final TravelProductMapper productMapper;
    private final TravelDayPlanMapper dayPlanMapper;
    private final DestinationMapper destinationMapper;
    private final AttractionMapper attractionMapper;
    private final HotelMapper hotelMapper;
    private final TransportMapper transportMapper;

    /**
     * 分页查询当前用户创建的独立路线。
     */
    public PageResult<TravelProduct> listUserRoutes(Long userId, Integer page, Integer size, String keyword) {
        Page<TravelProduct> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<TravelProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TravelProduct::getProviderId, userId)
                .eq(TravelProduct::getProductType, 3)
                .eq(TravelProduct::getStatus, 0)
                .eq(TravelProduct::getTags, USER_ROUTE_TAG);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(TravelProduct::getTitle, keyword);
        }
        wrapper.orderByDesc(TravelProduct::getUpdateTime).orderByDesc(TravelProduct::getCreateTime);
        Page<TravelProduct> result = productMapper.selectPage(pageParam, wrapper);

        Set<Long> destinationIds = result.getRecords().stream()
                .map(TravelProduct::getDestinationId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, Destination> destinationMap = destinationIds.isEmpty()
                ? Collections.emptyMap()
                : destinationMapper.selectBatchIds(destinationIds).stream()
                .collect(Collectors.toMap(Destination::getId, d -> d));

        result.getRecords().forEach(route -> {
            Destination destination = destinationMap.get(route.getDestinationId());
            if (destination != null) {
                route.setDestinationName(destination.getName());
            }
        });

        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 查询路线详情（包含每日行程及关联资源名称/坐标）。
     *
     * 处理步骤：
     * 1. 校验路线归属；
     * 2. 读取并排序日计划；
     * 3. 收集景点/酒店/交通 ID 后批量查询，避免循环单查；
     * 4. 组装成前端可直接渲染的详情结构。
     */
    public RouteDetail getRouteDetail(Long userId, Long routeId) {
        TravelProduct route = getOwnedRoute(userId, routeId);
        List<TravelDayPlan> dayPlans = dayPlanMapper.selectList(
                new LambdaQueryWrapper<TravelDayPlan>()
                        .eq(TravelDayPlan::getProductId, routeId)
                        .orderByAsc(TravelDayPlan::getDayNumber)
        );

        Set<Long> attractionIds = new LinkedHashSet<>();
        Set<Long> hotelIds = new LinkedHashSet<>();
        Set<Long> transportIds = new LinkedHashSet<>();
        for (TravelDayPlan dayPlan : dayPlans) {
            attractionIds.addAll(parseAttractionIds(dayPlan.getAttractionIds()));
            if (dayPlan.getHotelId() != null) {
                hotelIds.add(dayPlan.getHotelId());
            }
            if (dayPlan.getTransportId() != null) {
                transportIds.add(dayPlan.getTransportId());
            }
        }

        Map<Long, Attraction> attractionMap = attractionIds.isEmpty()
                ? Collections.emptyMap()
                : attractionMapper.selectBatchIds(attractionIds).stream()
                .collect(Collectors.toMap(Attraction::getId, a -> a));
        Map<Long, Hotel> hotelMap = hotelIds.isEmpty()
                ? Collections.emptyMap()
                : hotelMapper.selectBatchIds(hotelIds).stream()
                .collect(Collectors.toMap(Hotel::getId, h -> h));
        Map<Long, Transport> transportMap = transportIds.isEmpty()
                ? Collections.emptyMap()
                : transportMapper.selectBatchIds(transportIds).stream()
                .collect(Collectors.toMap(Transport::getId, t -> t));

        RouteDetail detail = new RouteDetail();
        detail.setId(route.getId());
        detail.setTitle(route.getTitle());
        detail.setDescription(route.getDescription());
        detail.setDestinationId(route.getDestinationId());
        Destination destination = route.getDestinationId() == null ? null : destinationMapper.selectById(route.getDestinationId());
        detail.setDestinationName(destination == null ? null : destination.getName());
        detail.setDuration(route.getDuration());

        List<RouteDayPlanDetail> routeDayPlans = new ArrayList<>();
        for (TravelDayPlan dayPlan : dayPlans) {
            RouteDayPlanDetail d = new RouteDayPlanDetail();
            d.setDayNumber(dayPlan.getDayNumber());
            d.setTitle(dayPlan.getTitle());
            d.setDescription(dayPlan.getDescription());

            List<Long> dayAttractionIds = parseAttractionIds(dayPlan.getAttractionIds());
            List<ResourceOption> attractions = dayAttractionIds.stream()
                    .map(attractionMap::get)
                    .filter(a -> a != null)
                    .map(a -> new ResourceOption(a.getId(), a.getName(), a.getTicketPrice(), a.getLongitude(), a.getLatitude()))
                    .collect(Collectors.toList());
            d.setAttractions(attractions);
            d.setAttractionIds(dayAttractionIds);

            d.setHotelId(dayPlan.getHotelId());
            Hotel hotel = dayPlan.getHotelId() == null ? null : hotelMap.get(dayPlan.getHotelId());
            d.setHotelName(hotel == null ? null : hotel.getName());

            d.setTransportId(dayPlan.getTransportId());
            Transport transport = dayPlan.getTransportId() == null ? null : transportMap.get(dayPlan.getTransportId());
            d.setTransportName(transport == null ? null : transport.getDeparture() + " -> " + transport.getArrival());
            routeDayPlans.add(d);
        }
        detail.setDayPlans(routeDayPlans);
        return detail;
    }

    /**
     * 创建独立路线（主表+日计划，事务提交）。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createRoute(Long userId, RouteSaveRequest request) {
        validateRouteRequest(request);

        TravelProduct route = new TravelProduct();
        route.setProviderId(userId);
        applyRouteFields(route, request);
        productMapper.insert(route);

        saveDayPlans(route.getId(), request.getDayPlans());
        return route.getId();
    }

    /**
     * 更新独立路线。
     *
     * 这里采用“先删后插”同步日计划，确保前后端结构保持一致，避免脏数据残留。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRoute(Long userId, Long routeId, RouteSaveRequest request) {
        validateRouteRequest(request);

        TravelProduct route = getOwnedRoute(userId, routeId);
        applyRouteFields(route, request);
        route.setId(routeId);
        productMapper.updateById(route);

        dayPlanMapper.delete(new LambdaQueryWrapper<TravelDayPlan>().eq(TravelDayPlan::getProductId, routeId));
        saveDayPlans(routeId, request.getDayPlans());
    }

    /**
     * 删除独立路线（同时删除关联日计划）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoute(Long userId, Long routeId) {
        getOwnedRoute(userId, routeId);
        dayPlanMapper.delete(new LambdaQueryWrapper<TravelDayPlan>().eq(TravelDayPlan::getProductId, routeId));
        productMapper.deleteById(routeId);
    }

    /**
     * 获取路线编辑所需资源候选集（景点/酒店/交通）。
     * destinationId 为空时返回全局候选，否则按目的地过滤景点与酒店。
     */
    public RouteResources getResources(Long destinationId) {
        LambdaQueryWrapper<Attraction> attractionWrapper = new LambdaQueryWrapper<>();
        attractionWrapper.eq(Attraction::getStatus, 1);
        if (destinationId != null) {
            attractionWrapper.eq(Attraction::getDestinationId, destinationId);
        }
        attractionWrapper.orderByDesc(Attraction::getCreateTime);
        List<ResourceOption> attractions = attractionMapper.selectList(attractionWrapper).stream()
                .map(a -> new ResourceOption(a.getId(), a.getName(), a.getTicketPrice(), a.getLongitude(), a.getLatitude()))
                .collect(Collectors.toList());

        LambdaQueryWrapper<Hotel> hotelWrapper = new LambdaQueryWrapper<>();
        hotelWrapper.eq(Hotel::getStatus, 1);
        if (destinationId != null) {
            hotelWrapper.eq(Hotel::getDestinationId, destinationId);
        }
        hotelWrapper.orderByDesc(Hotel::getCreateTime);
        List<ResourceOption> hotels = hotelMapper.selectList(hotelWrapper).stream()
                .map(h -> new ResourceOption(h.getId(), h.getName(), h.getPriceMin(), h.getLongitude(), h.getLatitude()))
                .collect(Collectors.toList());

        LambdaQueryWrapper<Transport> transportWrapper = new LambdaQueryWrapper<>();
        transportWrapper.eq(Transport::getStatus, 1);
        transportWrapper.orderByDesc(Transport::getCreateTime).last("LIMIT 200");
        List<TransportOption> transports = transportMapper.selectList(transportWrapper).stream()
                .map(t -> {
                    TransportOption option = new TransportOption();
                    option.setId(t.getId());
                    option.setName(t.getDeparture() + " -> " + t.getArrival());
                    option.setPrice(t.getPrice());
                    option.setType(t.getType());
                    return option;
                })
                .collect(Collectors.toList());

        RouteResources resources = new RouteResources();
        resources.setAttractions(attractions);
        resources.setHotels(hotels);
        resources.setTransports(transports);
        return resources;
    }

    /**
     * 校验并返回“当前用户拥有的独立路线”。
     * 同时校验类型、状态、标签，避免误操作到普通旅游产品。
     */
    private TravelProduct getOwnedRoute(Long userId, Long routeId) {
        TravelProduct route = productMapper.selectById(routeId);
        if (route == null
                || !userId.equals(route.getProviderId())
                || route.getProductType() == null
                || route.getProductType() != 3
                || route.getStatus() == null
                || route.getStatus() != 0
                || !USER_ROUTE_TAG.equals(route.getTags())) {
            throw new BusinessException("route not found");
        }
        return route;
    }

    /**
     * 路线保存请求基础校验。
     */
    private void validateRouteRequest(RouteSaveRequest request) {
        if (request == null) {
            throw new BusinessException("invalid route payload");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BusinessException("title is required");
        }
        if (request.getDestinationId() == null) {
            throw new BusinessException("destination is required");
        }
        if (request.getDuration() == null || request.getDuration() <= 0) {
            throw new BusinessException("duration is invalid");
        }
    }

    /**
     * 将请求字段映射到路线主表。
     * 独立路线不参与商品售卖，因此价格/库存/销量统一置为 0。
     */
    private void applyRouteFields(TravelProduct route, RouteSaveRequest request) {
        route.setTitle(request.getTitle());
        route.setDescription(request.getDescription());
        route.setDestinationId(request.getDestinationId());
        route.setDuration(request.getDuration());
        route.setPrice(BigDecimal.ZERO);
        route.setOriginalPrice(BigDecimal.ZERO);
        route.setProductType(3);
        route.setTags(USER_ROUTE_TAG);
        route.setIncludeItems("");
        route.setExcludeItems("");
        route.setStock(0);
        route.setSales(0);
        route.setStatus(0);
    }

    /**
     * 批量保存每日行程。
     *
     * 说明：
     * 1. 先按 dayNumber 排序，确保落库顺序稳定；
     * 2. 未传 dayNumber 时自动按顺序补齐；
     * 3. 景点 ID 列表使用逗号串落库，便于兼容现有表结构。
     */
    private void saveDayPlans(Long routeId, List<RouteDayPlanSaveRequest> dayPlans) {
        if (dayPlans == null || dayPlans.isEmpty()) {
            return;
        }
        List<RouteDayPlanSaveRequest> sorted = dayPlans.stream()
                .sorted((a, b) -> Integer.compare(
                        a.getDayNumber() == null ? Integer.MAX_VALUE : a.getDayNumber(),
                        b.getDayNumber() == null ? Integer.MAX_VALUE : b.getDayNumber()
                ))
                .collect(Collectors.toList());

        for (int i = 0; i < sorted.size(); i++) {
            RouteDayPlanSaveRequest day = sorted.get(i);
            TravelDayPlan entity = new TravelDayPlan();
            entity.setProductId(routeId);
            entity.setDayNumber(day.getDayNumber() == null ? i + 1 : day.getDayNumber());
            entity.setTitle(day.getTitle());
            entity.setDescription(day.getDescription());
            entity.setAttractionIds(joinAttractionIds(day.getAttractionIds()));
            entity.setHotelId(day.getHotelId());
            entity.setTransportId(day.getTransportId());
            dayPlanMapper.insert(entity);
        }
    }

    /**
     * 将景点 ID 列表编码为逗号分隔字符串。
     */
    private String joinAttractionIds(List<Long> attractionIds) {
        if (attractionIds == null || attractionIds.isEmpty()) {
            return null;
        }
        return attractionIds.stream()
                .filter(id -> id != null)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    /**
     * 将逗号分隔字符串解码为景点 ID 列表。
     */
    private List<Long> parseAttractionIds(String attractionIds) {
        if (attractionIds == null || attractionIds.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(attractionIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    @Data
    public static class RouteSaveRequest {
        private String title;
        private String description;
        private Long destinationId;
        private Integer duration;
        private List<RouteDayPlanSaveRequest> dayPlans;
    }

    @Data
    public static class RouteDayPlanSaveRequest {
        private Integer dayNumber;
        private String title;
        private String description;
        private List<Long> attractionIds;
        private Long hotelId;
        private Long transportId;
    }

    @Data
    public static class RouteDetail {
        private Long id;
        private String title;
        private String description;
        private Long destinationId;
        private String destinationName;
        private Integer duration;
        private List<RouteDayPlanDetail> dayPlans;
    }

    @Data
    public static class RouteDayPlanDetail {
        private Integer dayNumber;
        private String title;
        private String description;
        private List<Long> attractionIds;
        private List<ResourceOption> attractions;
        private Long hotelId;
        private String hotelName;
        private Long transportId;
        private String transportName;
    }

    @Data
    public static class RouteResources {
        private List<ResourceOption> attractions;
        private List<ResourceOption> hotels;
        private List<TransportOption> transports;
    }

    @Data
    public static class ResourceOption {
        private Long id;
        private String name;
        private BigDecimal price;
        private java.math.BigDecimal longitude;
        private java.math.BigDecimal latitude;

    public ResourceOption(Long id, String name, BigDecimal price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }

    public ResourceOption(Long id, String name, BigDecimal price,
                              java.math.BigDecimal longitude, java.math.BigDecimal latitude) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.longitude = longitude;
            this.latitude = latitude;
        }
    }

    @Data
    public static class TransportOption {
        private Long id;
        private String name;
        private BigDecimal price;
        private Integer type;
    }
}
