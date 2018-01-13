package me.kenzierocks.ourtube.response;

import java.util.Optional;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import me.kenzierocks.ourtube.AsyncService;

public interface Response<T> {

    static <T> ListenableFuture<Response<T>> from(ListenableFuture<T> future) {
        ListenableFuture<Response<T>> partial = Futures.transform(future, GoodResponse::make, AsyncService.GENERIC);
        return Futures.catching(partial, IllegalArgumentException.class, ErrorResponse::make, AsyncService.GENERIC);
    }

    Optional<String> getError();

    Optional<T> getValue();

}
