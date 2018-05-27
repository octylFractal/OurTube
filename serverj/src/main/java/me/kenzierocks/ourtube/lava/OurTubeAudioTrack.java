package me.kenzierocks.ourtube.lava;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;

import com.sedmelluq.discord.lavaplayer.filter.AudioPipeline;
import com.sedmelluq.discord.lavaplayer.filter.AudioPipelineFactory;
import com.sedmelluq.discord.lavaplayer.filter.PcmFormat;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BaseAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;

public class OurTubeAudioTrack extends BaseAudioTrack {

    private final AudioInputStream stream;
    private final InputStream bufferedStream;
    private final ShortBuffer inputBuffer;

    public OurTubeAudioTrack(AudioTrackInfo trackInfo, AudioInputStream stream) {
        super(trackInfo);
        checkArgument(stream.getFormat().getEncoding() == Encoding.PCM_SIGNED, "Need PCM encoding");
        this.inputBuffer = ByteBuffer.allocateDirect(2048 * stream.getFormat().getChannels())
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        this.stream = stream;
        bufferedStream = new BufferedInputStream(stream);
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        AudioProcessingContext ctx = executor.getProcessingContext();
        AudioPipeline downstream = AudioPipelineFactory.create(ctx,
                new PcmFormat(
                        stream.getFormat().getChannels(),
                        (int) stream.getFormat().getSampleRate()));
        try {
            executor.executeProcessingLoop(() -> {
                boolean eos = false;
                while (!eos) {
                    try {
                        eos = fillBuffer();
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }

                    downstream.process(inputBuffer);
                }
                downstream.flush();
            }, null);
        } finally {
            downstream.close();
        }
    }

    private boolean fillBuffer() throws IOException {
        inputBuffer.clear();
        boolean eos = false;
        while (inputBuffer.hasRemaining()) {
            int hi = bufferedStream.read();
            int lo = bufferedStream.read();
            if (lo == -1 || hi == -1) {
                eos = true;
                break;
            }
            inputBuffer.put((short) (lo | (hi << 8)));
        }
        inputBuffer.flip();
        return eos;
    }

}
