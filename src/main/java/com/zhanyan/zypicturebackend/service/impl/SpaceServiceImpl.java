package com.zhanyan.zypicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhanyan.zypicturebackend.exception.BusinessException;
import com.zhanyan.zypicturebackend.exception.ErrorCode;
import com.zhanyan.zypicturebackend.exception.ThrowUtils;
import com.zhanyan.zypicturebackend.model.dto.space.SpaceAddRequest;
import com.zhanyan.zypicturebackend.model.dto.space.SpaceQueryRequest;
import com.zhanyan.zypicturebackend.model.entity.Space;
import com.zhanyan.zypicturebackend.model.entity.User;
import com.zhanyan.zypicturebackend.model.enums.SpaceLevelEnum;
import com.zhanyan.zypicturebackend.model.vo.SpaceVO;
import com.zhanyan.zypicturebackend.model.vo.UserVO;
import com.zhanyan.zypicturebackend.service.SpaceService;
import com.zhanyan.zypicturebackend.mapper.SpaceMapper;
import com.zhanyan.zypicturebackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
* @author xiaoh
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2026-01-07 15:20:50
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;

    private Map<Long, Object> lockMap = new ConcurrentHashMap<>();


    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 复制对象属性
        Space space = new Space();
        BeanUtil.copyProperties(spaceAddRequest, space);
        // 数据校验
        String spaceName = space.getSpaceName();
        if (StrUtil.isBlank(spaceName)) {
            spaceName = "默认空间";
        }

        Integer spaceLevel = space.getSpaceLevel();
        if (spaceLevel == null) {
            spaceLevel = SpaceLevelEnum.COMMON.getValue();
        }
        // 填充参数
        this.fillSpaceBySpaceLevel(space);
        // 参数校验
        this.validSpace(space, true);
        Long userId = loginUser.getId();
        space.setUserId(userId);
        // 权限校验
        if (SpaceLevelEnum.COMMON.getValue() != spaceLevel && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别空间");
        }

        // 根据用户id加锁，防止用户创建多个空间
        Object lock = lockMap.computeIfAbsent(userId, key -> new Object());
        synchronized (lock) {
            Long newSpaceId = transactionTemplate.execute(status -> {
                // 判断用户是否存在空间
                boolean exists = this.lambdaQuery().eq(Space::getUserId, userId).exists();
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "用户已存在空间");
                // 写入数据
                boolean save = this.save(space);
                ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR);
                return space.getId();
            });
            return newSpaceId;
        }
    }

    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);

        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum enumByValue = SpaceLevelEnum.getEnumByValue(spaceLevel);
        // 如果是创建
        if (add) {
            ThrowUtils.throwIf(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            ThrowUtils.throwIf(enumByValue == null, ErrorCode.PARAMS_ERROR, "空间级别不能为空");
        }
        // 如果是修改
        ThrowUtils.throwIf(StrUtil.isNotBlank(spaceName) && spaceName.length() > 30, ErrorCode.PARAMS_ERROR, "空间名称过长");
        ThrowUtils.throwIf(spaceLevel != null && enumByValue == null, ErrorCode.PARAMS_ERROR, "空间级别不存在");
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别，自动填充限额
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUserId(userId);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    @Override
    public Wrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        Long userId = spaceQueryRequest.getUserId();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();

        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }
}




