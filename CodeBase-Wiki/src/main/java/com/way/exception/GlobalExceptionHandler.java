package com.way.exception;

import com.way.model.vo.ResponseVo;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author way
 * @description: 全局异常处理
 * @date 2025/7/20 16:10
 */
@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseVo<String> handleException(Exception e) {
        return ResponseVo.fail(500,e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseVo<String> handleRuntimeException(RuntimeException e){
        return ResponseVo.fail(400,e.getMessage());
    }
}
