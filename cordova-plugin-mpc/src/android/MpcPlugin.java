/**
 * MPD (Music Player Daemon) Client Plugin for Cordova.
 * Copyright (c) 2019, CLOUDSEAT Inc.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses>.
 *
 * @author AiChen
 * @copyright (c) 2019, CLOUDSEAT Inc.
 * @license https://www.gnu.org/licenses
 * @link https://www.cloudseat.net
 */

package net.cloudseat.cordova;

import android.content.Intent;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONException;

/**
 * Plugin Main Class
 */
public class MpcPlugin extends CordovaPlugin {

    // MPD message FLAGS
    private static final String MPD_VERSION = "OK MPD ";
    private static final String MPD_OK = "OK";
    private static final String MPD_ERROR = "ACK";

    // TCP socket
    private Socket socket;

    /**
     * Plugin main method
     */
    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callback)
        throws JSONException {

        switch (action) {
            case "connect": connect(args, callback); break;
            case "command": command(args, callback); break;
            case "disconnect": disconnect(callback); break;
            case "moveToBackground": moveToBackground(callback); break;
            default: callback.error("Undefined method:" + action);
            return false;
        }
        return true;
    }

    /**
     * Connect MPD server with host and port.
     */
    private void connect(CordovaArgs args, CallbackContext callback)
        throws JSONException  {
        String host = args.getString(0);
        int port = args.getInt(1);

        try {
            if (!isSocketAlive() || serverChanged(host, port))
            socket = new Socket(host, port);
            callback.success();
        } catch (Exception e) {
            callback.error(e.getMessage());
        }
    }

    /**
     * Write command to MPD server.
     * @link https://www.musicpd.org
     */
    private void command(CordovaArgs args, CallbackContext callback)
        throws JSONException  {
        String command = args.getString(0) + "\n";

        try {
            socket.getOutputStream().write(command.getBytes("UTF-8"));
            callback.success(receiveMessage());
        } catch (Exception e) {
            callback.error(e.getMessage());
        }
    }

    /**
     * Disconnect MPD server
     */
    private void disconnect(CallbackContext callback)  {
        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
            callback.success();
        } catch (Exception e) {
            callback.error(e.getMessage());
        }
    }

    /**
     * Moves the app to the background.
     */
    private void moveToBackground(CallbackContext callback) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        cordova.getActivity().startActivity(intent);
        callback.success();
    }

    /**
     * Receive message from MPD server.
     * Truncate input stream with MPD FLAGS
     */
    private String receiveMessage() throws Exception {
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String line;

        while (!isMessageEnd(line = reader.readLine())) {
            if (line.startsWith(MPD_ERROR)) {
                throw new Exception(line);
            }
            if (!line.startsWith(MPD_VERSION)) {
                // line = new String(line.getBytes("utf-8"), "utf-8");
                buffer.append(line + "\n");
            }
        }
        return buffer.toString();
    }

    /**
     * Check message end by read line.
     */
    private boolean isMessageEnd(String line) {
        return line == null || MPD_OK.equals(line);
    }

    /**
     * Check server changed
     */
    private boolean serverChanged(String host, int port) {
        String inetAddress = socket.getInetAddress().getHostAddress();
        int inetPort = socket.getPort();
        return inetPort != port || !inetAddress.equals(host);
    }

    /**
     * Check the real state of socket by urgent package
     */
    private boolean isSocketAlive() {
        if (socket == null || socket.isClosed() ||
            !socket.isConnected() || !socket.isBound()) {
            return false;
        }
        try {
            socket.sendUrgentData(0xFF);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
