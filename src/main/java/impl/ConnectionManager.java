package impl;

import java.net.Socket;

/**
 * Created by mallem on 4/19/16.
 */
public class ConnectionManager implements Runnable {

    private final Socket socket;

    public ConnectionManager(Socket socket) {
        this.socket = socket;
    }

    public void run() {

    }
}
