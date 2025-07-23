package com.hxg.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * @author hxg
 * @description: 文件存储接口
 * @date 2025/7/20 19:11
 */
public interface IFileService {
    /**
     * 获取文件树
     * @param localPath 本地路径
     * @return 文件树
     */
    public String getFileTree(String localPath);

    /**
     * 解压文件
     * @param file 文件
     * @param userName 用户名
     * @param projectName 项目名
     * @return 解压后的文件路径
     */
    public String unzipToProjectDir(MultipartFile file,String userName,String projectName);

    /**
     * 获取项目路径
     * @param userName 用户名
     * @param projectName 项目名
     * @return 项目路径
     */
    public String getRepositoryPath(String userName,String projectName);

    /**
     * 删除项目目录
     * @param userName 用户名
     * @param projectName 项目名
     */
    public void deleteProjectDirectory(String userName, String projectName);
}
