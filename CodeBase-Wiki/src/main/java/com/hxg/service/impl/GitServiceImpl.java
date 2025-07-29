package com.hxg.service.impl;

import com.hxg.model.param.CreateTaskParams;
import com.hxg.service.IGitService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * @author hxg
 * @description: git 服务实现类
 * @date 2025/7/20 19:05
 */
@Service
@Slf4j
public class GitServiceImpl implements IGitService {
    @Override
    public String cloneRepository(CreateTaskParams createTaskParams, String localPath) {
        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(createTaskParams.getProjectUrl())
                .setDirectory(new File(localPath));
        if (createTaskParams.getBranch() != null && !createTaskParams.getBranch().isEmpty()) {
            cloneCommand.setBranch(createTaskParams.getBranch());
        }
        if(createTaskParams.getUserName()!=null&&!createTaskParams.getUserName().isEmpty()){
            cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(createTaskParams.getUserName(), createTaskParams.getPassword()));
        }
        try {
            cloneCommand.call().close();
        } catch (Exception e) {
            throw new RuntimeException("克隆仓库失败：" + e.getMessage(), e);
        }
        return localPath;
    }
}
