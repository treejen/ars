package com.hktv.ars.data.base;


import com.hktv.ars.enums.CustomLogMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResultData<T> {

    private int status;
    private String message;
    private T data;

    public static <T> ResultData<T> success(T data) {

        ResultData<T> resultData = new ResultData<>();

        resultData.setStatus(HttpStatus.OK.value());
        resultData.setMessage(CustomLogMessage.SUCCESS.getLogMessage());
        resultData.setData(data);

        return resultData;
    }

    public static <T> ResultData<T> fail(int code, String message) {

        ResultData<T> resultData = new ResultData<>();
        resultData.setStatus(code);
        resultData.setMessage(message);

        return resultData;
    }
}
