package me.kenzierocks.ourtube.response;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ErrorResponse<T> implements Response<T> {

    public static <T> ErrorResponse<T> make(Throwable error) {
        return make(error.getLocalizedMessage());
    }

    public static <T> ErrorResponse<T> make(String error) {
        return new AutoValue_ErrorResponse<>(error);
    }

    ErrorResponse() {
    }

    @Override
    public Optional<String> getError() {
        return Optional.of(getGuarenteedError());
    }

    @Deprecated
    @Override
    public Optional<T> getValue() {
        return Optional.empty();
    }

    @JsonIgnore
    public abstract String getGuarenteedError();

}
