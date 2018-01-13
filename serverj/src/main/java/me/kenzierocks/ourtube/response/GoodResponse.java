package me.kenzierocks.ourtube.response;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class GoodResponse<T> implements Response<T> {

    public static <T> GoodResponse<T> make(T value) {
        return new AutoValue_GoodResponse<>(value);
    }

    GoodResponse() {
    }

    @Deprecated
    @Override
    public Optional<String> getError() {
        return Optional.empty();
    }

    @Override
    public Optional<T> getValue() {
        return Optional.of(getGuarenteedValue());
    }

    @JsonIgnore
    public abstract T getGuarenteedValue();

}
