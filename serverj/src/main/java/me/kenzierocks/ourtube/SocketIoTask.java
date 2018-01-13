package me.kenzierocks.ourtube;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.protocol.JacksonJsonSupport;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

public class SocketIoTask implements Runnable {

    @Override
    public void run() {
        Configuration config = new Configuration();
        config.setPort(13445);
        config.setContext("/transport-layer");
        config.setJsonSupport(new JacksonJsonSupport(
                new Jdk8Module(),
                new GuavaModule()));
        config.getSocketConfig().setReuseAddress(true);
        SocketIOServer server = new SocketIOServer(config);
        new OurTubeApi(server.addNamespace("/api")).configure();
        server.startAsync().addListener(new FutureListener<Void>() {

            @Override
            public void operationComplete(Future<Void> future) throws Exception {
                if (!future.isSuccess()) {
                    System.exit(1);
                }
            }
        });
    }

}
