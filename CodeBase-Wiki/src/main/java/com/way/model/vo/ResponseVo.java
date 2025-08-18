package com.way.model.vo;

import lombok.Getter;

/**
 * @author way
 * @date 2025/7/20 16:15
 */
@Getter
public class ResponseVo<T> {
    private int code;
    private String msg;
    private T data;

    private ResponseVo(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> ResponseVo<T> success(T data) {
        return new ResponseVo<>(200, "success", data);
    }

    public static <T> ResponseVo<T> success() {
        return new ResponseVo<>(200, "success", null);
    }

    public static <T> ResponseVo<T> success(String msg) {
        return new ResponseVo<>(200, msg, null);
    }

    public static <T> ResponseVo<T> success(String msg, T data) {
        return new ResponseVo<>(200, msg, data);
    }

    public static <T> ResponseVo<T> fail(int code, String msg) {
        return new ResponseVo<>(code, msg, null);
    }

    public static <T> ResponseVo<T> fail(int code, String msg, T data) {
        return new ResponseVo<>(code, msg, data);
    }

    public static <T> ResponseVo<T> fail(String msg) {
        return new ResponseVo<>(500, msg, null);
    }

    public static <T> ResponseVo<T> fail(String msg, T data) {
        return new ResponseVo<>(500, msg, data);
    }

}
