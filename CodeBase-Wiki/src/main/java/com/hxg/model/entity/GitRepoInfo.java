package com.hxg.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author hxg
 * @description: Git仓库信息
 * @date 2025/7/20 16:28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitRepoInfo {

    private String gitRepoUrl;
    private String gitUserName;
    private String gitPassword;

}
