package me.kenzierocks.ourtube;

public class OurTube {

    public static void main(String[] args) throws InterruptedException {
        // trigger ws
        AsyncService.GENERIC.submit(new SocketIoTask());
        // trigger bot
        Dissy.BOT.online();
        // sit and wait to die
        Thread.sleep(Long.MAX_VALUE);
    }

}
