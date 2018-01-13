package me.kenzierocks.ourtube;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class SongData {

    @AutoValue
    public abstract static class Thumbnail {

        public static Thumbnail of(long height, long width, String url) {
            return new AutoValue_SongData_Thumbnail(height, width, url);
        }

        Thumbnail() {
        }

        public abstract long getHeight();

        public abstract long getWidth();

        public abstract String getUrl();

    }

    public static Builder builder() {
        return new AutoValue_SongData.Builder();
    }

    @AutoValue.Builder
    public interface Builder {

        Builder id(String id);

        Builder name(String name);

        Builder thumbnail(Thumbnail thumbnail);

        Builder duration(long duration);

        SongData build();

    }

    SongData() {
    }

    public abstract String getId();

    public abstract String getName();

    public abstract Thumbnail getThumbnail();

    public abstract long getDuration();

}
