package net.cloudseat.smbova;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

import java.io.IOException;
import java.util.Enumeration;

public class WakeOnLan {

    public static final int PORT = 9;
    public static final String MAC_RULE = "([0-9a-fA-F]{2}[-:]){5}[0-9a-fA-F]{2}";

    /**
     * 发送UDP数据包到广播地址
     * @param macAddress 需要唤醒的机器的MAC地址
     * @return
     */
    public static void broadcast(String macAddress)
        throws UnknownHostException, SocketException, IOException {
        broadcast(macAddress, PORT);
    }

    /**
     * 发送UDP数据包到广播地址
     * @param macAddress 需要唤醒的机器的MAC地址
     * @param port 需要唤醒的机器的端口
     * @return
     */
    public static void broadcast(String macAddress, int port)
        throws UnknownHostException, SocketException, IOException {
        broadcast(getBroadcastIp(), macAddress, port == 0 ? PORT : port);
    }

    /**
     * 发送UDP数据包到广播地址
     * @param broadcastIp 广播IP地址
     * @param macAddress 需要唤醒的机器的MAC地址
     * @return
     */
    public static void broadcast(String broadcastIp, String macAddress) throws IOException {
        broadcast(broadcastIp, macAddress, PORT);
    }

    /**
     * 发送UDP数据包到广播地址
     * @param broadcastIp 广播IP地址
     * @param macAddress 需要唤醒的机器的MAC地址
     * @param port 端口号（默认：9）
     * @return
     */
    public static void broadcast(String broadcastIp, String macAddress, int port) throws IOException {
        byte[] macBytes = getMacBytes(macAddress);
        byte[] command = getMagicPacket(macBytes);

        InetAddress address = InetAddress.getByName(broadcastIp);
        DatagramPacket packet = new DatagramPacket(command, command.length, address, port);
        DatagramSocket socket = new DatagramSocket();
        socket.send(packet);
        socket.close();
    }

    /**
     * 根据本机地址自动获取局域网广播地址
     * @return 广播地址IP字符串
     */
    private static String getBroadcastIp() throws UnknownHostException, SocketException {
        for (Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            netInterfaces.hasMoreElements();) {

            NetworkInterface netInterface = netInterfaces.nextElement();
            if (!netInterface.isLoopback()) {
                for (InterfaceAddress interfaceAddress : netInterface.getInterfaceAddresses()) {
                    if (interfaceAddress.getBroadcast() != null) {
                        return interfaceAddress.getBroadcast().getHostAddress();
                    }
                }
            }
        }
        return null;
    }

    /**
     * 将MAC字符串转换为byte数组
     * @param macStr 字符串型MAC地址
     * @return byte数组型MAC地址
     */
    private static byte[] getMacBytes(String macStr) {
        // 验证MAC合法性
        if (macStr == null || !macStr.matches(MAC_RULE)) {
            throw new IllegalArgumentException("Invalid MAC address.");
        }

        String[] macHex = macStr.split("(\\:|\\-)");
        byte[] bytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            bytes[i] = (byte) Integer.parseInt(macHex[i], 16);
        }
        return bytes;
    }

    /**
     * 构建广播魔术包
     * 魔术包由固定前缀即6对“FF”和重复16次的MAC地址组成，进行UDP广播时需转换为二进制byte数组。
     * @param macBytes byte数组型MAC地址
     * @return byte数组型数据包
     */
    private static byte[] getMagicPacket(byte[] macBytes) {
        byte[] bytes = new byte[6 + 16 * macBytes.length];
        // 6次重复的“FF”
        for (int i = 0; i < 6; i++) {
            bytes[i] = (byte) 0xff;
        }
        // 16次重复的MAC地址
        for (int i = 6; i < bytes.length; i += macBytes.length) {
            System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
        }
        return bytes;
    }

}
