package com.hktv.ars.handler;


import com.hktv.ars.constant.LogConstant;
import com.hktv.ars.data.base.ResultData;
import com.hktv.ars.enums.CustomErrorLogMessage;
import com.hktv.ars.exception.CustomRuntimeException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@ControllerAdvice(basePackages = {"com.hktv.ars.controller"})
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final static String INVALID_ARGUMENTS_PREFIX = "Invalid Arguments : ";

    @ExceptionHandler({CustomRuntimeException.class})
    @ResponseBody
    public ResultData<String> handleCustomRuntimeException(final CustomRuntimeException cex) {

        int errorCode = cex.getErrorCode();
        String logMessage = (cex.getLogMessage() != null) ? cex.getLogMessage() : "None";

        log.error(LogConstant.ALERT_PREFIX + "CustomRuntimeException error code: {}, failed log message: {}", errorCode, logMessage);
        log.error("CustomRuntimeException exception:", cex);

        return ResultData.fail(errorCode, logMessage);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        String resultMessage = handleArgumentRelatedExceptionMessage(ex);

        log.error(LogConstant.ALERT_PREFIX + resultMessage);

        return new ResponseEntity<>(ResultData.fail(CustomErrorLogMessage.METHOD_ARGUMENT_ERROR.getCode(), resultMessage), HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleConversionNotSupported(ConversionNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Object[] args = new Object[]{ex.getPropertyName(), ex.getValue()};
        String resultMessage = "Failed to convert '" + args[0] + "' with value: '" + args[1] + "'";
        return new ResponseEntity<>(ResultData.fail(CustomErrorLogMessage.METHOD_ARGUMENT_ERROR.getCode(), resultMessage), HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Object[] args = new Object[]{ex.getPropertyName(), ex.getValue()};
        String resultMessage = "Failed to convert '" + args[0] + "' with value: '" + args[1] + "'";
        return new ResponseEntity<>(ResultData.fail(CustomErrorLogMessage.METHOD_ARGUMENT_ERROR.getCode(), resultMessage), HttpStatus.BAD_REQUEST);
    }

    private String handleArgumentRelatedExceptionMessage(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
            return INVALID_ARGUMENTS_PREFIX.concat(handleErrors(methodArgumentNotValidException));
        } else if (ex instanceof ConstraintViolationException constraintViolationException) {
            return INVALID_ARGUMENTS_PREFIX.concat(handleConstraintViolations(constraintViolationException));
        } else if (ex instanceof BindException bindException) {
            return INVALID_ARGUMENTS_PREFIX.concat(handleErrors(bindException));
        } else if (ex instanceof MissingServletRequestParameterException missingServletRequestParameterException) {
            return INVALID_ARGUMENTS_PREFIX.concat(missingServletRequestParameterException.getParameterName());
        } else if (ex instanceof HttpMessageNotReadableException) {
            return INVALID_ARGUMENTS_PREFIX.concat(ex.getLocalizedMessage());
        }
        return INVALID_ARGUMENTS_PREFIX;
    }

    private String handleErrors(BindException bindException) {
        return bindException.getAllErrors().stream()
                .map(error -> {
                    String fieldName = ((FieldError) error).getField();
                    String message = error.getDefaultMessage();
                    return fieldName + " -> " + message + "  ";
                })
                .collect(Collectors.joining(","));
    }

    private String handleConstraintViolations(ConstraintViolationException ex) {
        return ex.getConstraintViolations().stream()
                .map(error -> {
                    String fieldName = Objects.requireNonNull(StreamSupport.stream(error.getPropertyPath().spliterator(), false).reduce((first, second) -> second).orElse(null)).toString();
                    String message = error.getMessage();
                    return fieldName + " -> " + message + "  ";
                })
                .collect(Collectors.joining(","));
    }

}
