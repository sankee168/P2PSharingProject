package impl;

import utilities.PayloadWriter;

import java.net.Socket;

/**
 * Created by mallem on 4/19/16.
 */
public class ConnectionManager implements Runnable {

    private Socket socket;
    private PayloadWriter payloadWriter;

    public ConnectionManager(Socket socket) {
        this.socket = socket;
    }

    public void run() {

    }
}
