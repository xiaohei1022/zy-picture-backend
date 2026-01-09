package com.zhanyan.zypicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhanyan.zypicturebackend.model.dto.picture.*;
import com.zhanyan.zypicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhanyan.zypicturebackend.model.entity.User;
import com.zhanyan.zypicturebackend.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author xiaoh
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2026-01-04 16:58:38
*/
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    /**
     * 分页获取图片封装类
     *
     * @param picturePage
     * @param request
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 校验图片
     *
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 获取查询条件
     *
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取查询结果
     *
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充审核参数
     *
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    int uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    /**
     * 删除图片
     *
     * @param picture
     */
    void clearPictureCos(Picture  picture);

    /**
     * 检查图片权限
     *
     * @param loginUser
     * @param picture
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 删除图片
     *
     * @param pictureId
     * @param loginUser
     */
    void deletePicture(long pictureId, User loginUser);

    /**
     * 编辑图片
     *
     * @param pictureEditRequest
     * @param loginUser
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 更新图片
     *
     * @param pictureUpdateRequest
     * @param loginUser
     */
    void updatePicture(PictureUpdateRequest pictureUpdateRequest, User loginUser);
}
