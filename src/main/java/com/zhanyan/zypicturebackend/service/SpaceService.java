package com.zhanyan.zypicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhanyan.zypicturebackend.model.dto.space.SpaceAddRequest;
import com.zhanyan.zypicturebackend.model.dto.space.SpaceQueryRequest;
import com.zhanyan.zypicturebackend.model.entity.Space;
import com.zhanyan.zypicturebackend.model.entity.User;
import com.zhanyan.zypicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author xiaoh
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2026-01-07 15:25:03
*/
public interface SpaceService extends IService<Space> {

    /**
     * 添加空间
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 参数校验
     * @param space
     * @param add
     */
    void validSpace(Space space, boolean add);

    /**
     * 根据空间级别填充空间信息
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 获取空间视图
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取查询条件
     * @param spaceQueryRequest
     * @return
     */
    Wrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 获取空间视图列表
     * @param spacePage
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 检查空间权限
     *
     * @param space
     * @param loginUser
     */
    void checkSpaceAuth(Space space, User loginUser);
}
