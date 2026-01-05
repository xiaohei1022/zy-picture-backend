package com.zhanyan.zypicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhanyan.zypicturebackend.model.dto.picture.PictureQueryRequest;
import com.zhanyan.zypicturebackend.model.dto.picture.PictureUploadRequest;
import com.zhanyan.zypicturebackend.model.dto.picture.PictureVO;
import com.zhanyan.zypicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhanyan.zypicturebackend.model.entity.User;
import org.springframework.web.multipart.MultipartFile;

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
     * @param multipartFile
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(MultipartFile multipartFile,
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
}
