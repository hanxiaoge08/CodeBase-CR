package com.hxg.service;

import com.hxg.model.param.CreateTaskParams;

/**
 * @author hxg
 * @description: Git 服务接口
 * @date 2025/7/20 18:55
 */
public interface IGitService {
    /**
     * 克隆仓库
     * @param createTaskParams 创建任务参数
     * @param localPath 本地路径
     * @return 本地路径
     */
    public String cloneRepository(CreateTaskParams createTaskParams, String localPath);

}
