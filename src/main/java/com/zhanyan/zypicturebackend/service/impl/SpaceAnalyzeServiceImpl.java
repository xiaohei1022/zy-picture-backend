package com.zhanyan.zypicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhanyan.zypicturebackend.exception.BusinessException;
import com.zhanyan.zypicturebackend.exception.ErrorCode;
import com.zhanyan.zypicturebackend.exception.ThrowUtils;
import com.zhanyan.zypicturebackend.mapper.SpaceMapper;
import com.zhanyan.zypicturebackend.model.dto.space.analyze.*;
import com.zhanyan.zypicturebackend.model.entity.Picture;
import com.zhanyan.zypicturebackend.model.entity.Space;
import com.zhanyan.zypicturebackend.model.entity.User;
import com.zhanyan.zypicturebackend.model.vo.analyze.*;
import com.zhanyan.zypicturebackend.service.PictureService;
import com.zhanyan.zypicturebackend.service.SpaceAnalyzeService;
import com.zhanyan.zypicturebackend.service.SpaceService;
import com.zhanyan.zypicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceAnalyzeService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;


    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 检查权限
        checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
        // 获取查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceUsageAnalyzeRequest, queryWrapper);

        SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
        if (spaceUsageAnalyzeRequest.isQueryAll() || spaceUsageAnalyzeRequest.isQueryPublic()) {
            queryWrapper.select("picSize");
            // 查询
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            long userSize = pictureObjList.stream().mapToLong(obj -> (Long) obj).sum();
            long userCount = pictureObjList.size();
            spaceUsageAnalyzeResponse.setUsedSize(userSize);
            spaceUsageAnalyzeResponse.setUsedCount(userCount);
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
        } else {
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            Space space = spaceService.getById(spaceId);
            spaceUsageAnalyzeResponse.setUsedSize(space.getTotalSize());
            spaceUsageAnalyzeResponse.setUsedCount(space.getTotalCount());
            spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());
            spaceUsageAnalyzeResponse.setMaxCount(space.getMaxCount());
            // 计算百分比
            double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
            double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
        }
        return spaceUsageAnalyzeResponse;
    }

    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 权限校验
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);

        // 获取查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);
        queryWrapper.select("category AS category, COUNT(*) AS count, SUM(picSize) AS totalSize")
                .groupBy("category");

        // 查询
        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(map -> {
                    String category = map.get("category") != null ? map.get("category").toString() : "未分类";
                    long count = Long.parseLong(map.get("count").toString());
                    long totalSize = Long.parseLong(map.get("totalSize").toString());
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 权限校验
        checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);

        // 获取查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);

        queryWrapper.select("tags");
        List<String> pictureTagList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(ObjUtil::toString)
                .collect(Collectors.toList());

        // 扁平化标签字符串，统计使用次数
        Map<String, Long> tagCountMap = pictureTagList.stream()
                .flatMap(obj -> JSONUtil.toList(obj, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        return tagCountMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue())) // 降序
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 权限校验
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);

        // 获取查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);

        queryWrapper.select("picSize");
        // 查询
        List<Long> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .map(obj -> (Long) obj)
                .collect(Collectors.toList());

        // 定义大小范围, 使用 LinkedHashMap 有序map
        Map<String, Long> sizeRanges = new LinkedHashMap<>();
        sizeRanges.put("<100KB", pictureObjList.stream().filter(size -> size < 100 * 1024).count());
        sizeRanges.put("100KB-500KB", pictureObjList.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());
        sizeRanges.put("500KB-1MB", pictureObjList.stream().filter(size -> size >= 500 * 1024 && size < 1024 * 1024).count());
        sizeRanges.put(">1MB", pictureObjList.stream().filter(size -> size >= 1024 * 1024).count());
        return sizeRanges.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 权限校验
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);

        // 获取查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);
        Long userId = spaceUserAnalyzeRequest.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);

        // 分析时间维度：天、周、月
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') AS period, COUNT(*) AS count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) AS period, COUNT(*) AS count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') AS period, COUNT(*) AS count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "时间维度错误");
        }
        queryWrapper.groupBy("period").orderByAsc("period");
        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(map -> {
                    String period = map.get("period") != null ? map.get("period").toString() : "未知";
                    long count = Long.parseLong(map.get("count").toString());
                    return new SpaceUserAnalyzeResponse(period, count);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

        // 仅管理员可查看空间排行
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无权查看空间排行");

        // 构造查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "spaceName", "userId", "totalSize")
                .orderByDesc("totalSize")
                .last("LIMIT " + spaceRankAnalyzeRequest.getTopN()); // 取前 N 名

        // 查询结果
        return spaceService.list(queryWrapper);
    }

    /**
     * 检查空间权限
     *
     * @param spaceAnalyzeRequest
     * @param loginUser
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        // 检查权限
        if (spaceAnalyzeRequest.isQueryAll() || spaceAnalyzeRequest.isQueryPublic()) {
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "没有权限");
        } else {
            // 获取空间
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR, "空间 ID 不能为空");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            spaceService.checkSpaceAuth(space, loginUser);
        }
    }

    /**
     * 填充查询条件
     *
     * @param spaceAnalyzeRequest
     * @param queryWrapper
     */
    private void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        if (spaceAnalyzeRequest.isQueryAll()) {
            return;
        }
        if (spaceAnalyzeRequest.isQueryPublic()) {
            queryWrapper.isNull("spaceId");
            return;
        }
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
    }
}
