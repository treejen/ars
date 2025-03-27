package com.hktv.ars.exception;

public class HttpServerSideException extends RuntimeException {

    public HttpServerSideException(int statusCode, String msg) {

        super(String.format("RestTemplate client side error. Status code : [%s], Msg : [%s]", statusCode, msg));
    }
}
