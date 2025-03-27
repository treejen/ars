package com.hktv.ars.exception;

public class HttpClientSideException extends RuntimeException {

    public HttpClientSideException(int statusCode, String msg) {

        super(String.format("RestTemplate client side error. Status code : [%s], Msg : [%s]", statusCode, msg));
    }
}
