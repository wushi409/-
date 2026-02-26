package com.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.Constants;
import com.travel.common.PageResult;
import com.travel.common.exception.BusinessException;
import com.travel.entity.TravelNote;
import com.travel.entity.User;
import com.travel.mapper.TravelNoteMapper;
import com.travel.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 游记业务服务。
 */
@Service
@RequiredArgsConstructor
public class TravelNoteService {

    private final TravelNoteMapper travelNoteMapper;
    private final UserMapper userMapper;

    /**
     * 游记列表查询：支持关键词、目的地、仅看我的。
     */
    public PageResult<TravelNote> list(Integer page, Integer size, String keyword, String destination, Boolean onlyMine, Long userId) {
        Page<TravelNote> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<TravelNote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TravelNote::getStatus, Constants.TRAVEL_NOTE_PUBLISHED);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(TravelNote::getTitle, keyword)
                    .or().like(TravelNote::getContent, keyword)
                    .or().like(TravelNote::getDestinationName, keyword));
        }
        if (StringUtils.hasText(destination)) {
            wrapper.like(TravelNote::getDestinationName, destination);
        }
        if (Boolean.TRUE.equals(onlyMine)) {
            if (userId == null) {
                throw new BusinessException("onlyMine=true 时需要先登录");
            }
            wrapper.eq(TravelNote::getUserId, userId);
        }
        wrapper.orderByDesc(TravelNote::getCreatedAt);

        Page<TravelNote> result = travelNoteMapper.selectPage(pageParam, wrapper);
        fillAuthorInfo(result.getRecords());
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 发布游记。
     */
    public TravelNote create(Long userId, TravelNote travelNote) {
        if (travelNote == null) {
            throw new BusinessException("请求参数不能为空");
        }
        if (!StringUtils.hasText(travelNote.getTitle())) {
            throw new BusinessException("标题不能为空");
        }
        if (!StringUtils.hasText(travelNote.getDestinationName())) {
            throw new BusinessException("目的地不能为空");
        }
        if (travelNote.getTravelDate() == null) {
            throw new BusinessException("出行日期不能为空");
        }
        if (!StringUtils.hasText(travelNote.getContent())) {
            throw new BusinessException("游记正文不能为空");
        }

        Integer rating = travelNote.getRating() == null ? 5 : travelNote.getRating();
        if (rating < 1 || rating > 5) {
            throw new BusinessException("评分范围必须在 1~5 之间");
        }

        travelNote.setUserId(userId);
        travelNote.setRating(rating);
        travelNote.setStatus(Constants.TRAVEL_NOTE_PUBLISHED);
        travelNote.setCreatedAt(LocalDateTime.now());
        travelNote.setUpdateTime(LocalDateTime.now());
        travelNoteMapper.insert(travelNote);

        User user = userMapper.selectById(userId);
        travelNote.setAuthorId(userId);
        travelNote.setAuthorName(resolveNickname(user));
        return travelNote;
    }

    /**
     * 批量回填作者信息，避免前端再发额外请求。
     */
    private void fillAuthorInfo(List<TravelNote> notes) {
        Set<Long> userIds = notes.stream()
                .map(TravelNote::getUserId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }

        Map<Long, String> nicknameMap = userMapper.selectBatchIds(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, this::resolveNickname, (a, b) -> a));

        for (TravelNote note : notes) {
            note.setAuthorId(note.getUserId());
            note.setAuthorName(nicknameMap.getOrDefault(note.getUserId(), "匿名用户"));
        }
    }

    /**
     * 昵称为空时回退到用户名，保证展示字段不为空。
     */
    private String resolveNickname(User user) {
        if (user == null) {
            return "匿名用户";
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername();
        }
        return "匿名用户";
    }
}
