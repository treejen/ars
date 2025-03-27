package com.hktv.ars.handler;

import com.hktv.ars.exception.HttpClientSideException;
import com.hktv.ars.exception.HttpServerSideException;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

public class RestTemplateResponseErrorHandler implements ResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse httpResponse) throws IOException {

        return (httpResponse.getStatusCode().is4xxClientError()
                || httpResponse.getStatusCode().is5xxServerError());
    }

    @Override
    public void handleError(ClientHttpResponse httpResponse) throws IOException {

        if (httpResponse.getStatusCode().is5xxServerError()) {
            throw new HttpServerSideException(httpResponse.getStatusCode().value(), httpResponse.getBody().toString());

        } else if (httpResponse.getStatusCode().is4xxClientError()) {
            throw new HttpClientSideException(httpResponse.getStatusCode().value(), httpResponse.getBody().toString());
        }
    }
}
