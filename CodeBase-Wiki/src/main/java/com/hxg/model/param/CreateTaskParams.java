package com.hxg.model.param;

import lombok.Data;

/**
 * @author hxg
 * @description: TODO
 * @date 2025/7/20 18:58
 */
@Data
public class CreateTaskParams {

    private String projectName;

    private String projectUrl;

    private String branch;

    private String userName;

    private String password;

    // git或zip
    private String sourceType;

}
