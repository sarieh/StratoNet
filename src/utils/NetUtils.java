package utils;

import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class NetUtils {


    public static final int WELCOMING_AUTH_PORT = 80;
    public static final int WELCOMING_DATA_PORT = 88;

    public static void sendTcpMsg(DataOutputStream toClient, int type, int phase, String message) throws IOException {
        byte phase_ = intToByte(phase);
        byte type_ = intToByte(type);
        byte[] msg_ = message.getBytes();
        byte[] size_ = ByteBuffer.allocate(4).putInt(msg_.length).array();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(phase_);
        outputStream.write(type_);
        outputStream.write(size_);
        outputStream.write(msg_);
        toClient.write(outputStream.toByteArray());
    }

    public static void sendTcpImageMsg(DataOutputStream toClient, int type, int phase, BufferedImage image) throws IOException {
        byte phase_ = intToByte(phase);
        byte type_ = intToByte(type);
        byte[] image_ = toByteArray(image);
        byte[] hash_ = getImageHash(image).getBytes();
        byte[] hashSize_ = ByteBuffer.allocate(4).putInt(hash_.length).array();
        byte[] imgSize_ = ByteBuffer.allocate(4).putInt(image_.length).array();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(phase_);
        outputStream.write(type_);
        outputStream.write(imgSize_);
        outputStream.write(hashSize_);
        outputStream.write(hash_);
        outputStream.write(image_);
        outputStream.close();
        toClient.write(outputStream.toByteArray());
    }

    public static void sendTcpJsonMsg(DataOutputStream toClient, int type, int phase, JSONObject sol) throws IOException {
        byte phase_ = intToByte(phase);
        byte type_ = intToByte(type);
        byte[] hash_ = getJsonHash(sol).getBytes();
        byte[] hashSize_ = ByteBuffer.allocate(4).putInt(hash_.length).array();
        byte[] sol_ = sol.toString().getBytes();
        byte[] solSize_ = ByteBuffer.allocate(4).putInt(sol_.length).array();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(phase_);
        outputStream.write(type_);
        outputStream.write(solSize_);
        outputStream.write(hashSize_);
        outputStream.write(hash_);
        outputStream.write(sol_);
        outputStream.close();
        toClient.write(outputStream.toByteArray());
    }

    public static byte intToByte(int val) {
        return Integer.valueOf(val).byteValue();
    }

    public static byte[] toByteArray(BufferedImage bi) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(bi, "jpg", bos);
        bos.close();
        return bos.toByteArray();
    }

    public static String getImageHash(BufferedImage image) throws IOException {
        return (Arrays.hashCode(toByteArray(image)) + "").substring(0, 8);
    }

    public static String getJsonHash(JSONObject sol) {
        return ("" + sol.hashCode()).substring(0, 8);
    }

    public static HttpResponse<String> httpCall(String urlStr) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(urlStr)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response;
    }
}
