/*
 * This file is part of OurTube-serverj, licensed under the MIT License (MIT).
 *
 * Copyright (c) Kenzie Togami (kenzierocks) <https://kenzierocks.me>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package me.kenzierocks.ourtube.events;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

import com.google.auto.value.AutoValue;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import me.kenzierocks.ourtube.lava.OurTubeAudioTrack;
import me.kenzierocks.ourtube.lava.OurTubeAudioTrack.OurTubeMetadata;

@AutoValue
public abstract class SongQueuedEvent implements GuildEvent {

    public static Optional<SongQueuedEvent> fromTrack(String guildId, AudioTrack track) {
        return OurTubeAudioTrack.cast(track)
                .map(otat -> {
                    OurTubeMetadata md = otat.getMetadata();
                    return builder()
                            .guildId(guildId)
                            .dataId(md.dataId())
                            .queueId(otat.getIdentifier())
                            .queueTime(md.queueTime().toString())
                            .submitterId(md.submitter())
                            .build();
                });
    }

    public static Builder builder() {
        return new AutoValue_SongQueuedEvent.Builder();
    }

    @AutoValue.Builder
    public interface Builder {

        Builder guildId(String guildId);

        Builder dataId(String dataId);

        Builder queueId(String queueId);

        Builder queueTime(String queueTime);

        Builder submitterId(String submitterId);

        SongQueuedEvent build();

    }

    SongQueuedEvent() {
    }

    public abstract String getDataId();

    public abstract String getQueueId();

    /**
     * See {@link DateTimeFormatter#ISO_INSTANT} for format.
     */
    public abstract String getQueueTime();

    public abstract String getSubmitterId();

}
