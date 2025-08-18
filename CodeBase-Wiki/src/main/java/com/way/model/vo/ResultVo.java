package com.way.model.vo;

import lombok.Data;

/**
 * @author way
 * @description: TODO
 * @date 2025/7/23 21:48
 */
@Data
public class ResultVo <T>{
    private int code;
    private String msg;
    private T data;
    
    public static <T> ResultVo<T> success(T data) {
        ResultVo<T> result=new ResultVo<>();
        result.setCode(200);
        result.setMsg("success");
        result.setData(data);
        return result;
    }
    
    public static <T> ResultVo<T> error(String msg) {
        ResultVo<T> result=new ResultVo<>();
        result.setCode(500);
        result.setMsg(msg);
        return result;
    }
}
