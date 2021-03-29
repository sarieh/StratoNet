import org.json.JSONObject;
import utils.NetUtils;
import utils.TcpMessage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static utils.MsgPhase.*;
import static utils.MsgType.*;

class ClientThread extends Thread {
    private final Socket command_socket;
    private Socket data_socket;
    private final ServerSocket data_socket_server;
    private final ArrayList<String> userNames;
    private final ArrayList<String> passwords;
    private String hash;
    private DataInputStream fromClient;
    private DataOutputStream toClient;
    private DataOutputStream data_toClient;

    private static final String USER_PROMP = "commands: \n\tEnter \"1 date\" to get an image form Apod API (note: date of the format: 2020-05-05)\n\tEnter \"2\" to get a random Sol information\n\tEnter \"exit\" to exit";

    public ClientThread(Socket command_socket, ServerSocket data_socket_server, ArrayList<String> userNames, ArrayList<String> passwords) {
        this.command_socket = command_socket;
        this.data_socket_server = data_socket_server;
//        this.data_socket = data_socket;
        this.userNames = userNames;
        this.passwords = passwords;
    }

    public void run() {
        try {
            fromClient = new DataInputStream(command_socket.getInputStream());
            toClient = new DataOutputStream(command_socket.getOutputStream());
            if (authenticateUser(toClient, fromClient)) {
                data_socket = data_socket_server.accept();
                data_toClient = new DataOutputStream(data_socket.getOutputStream());
                acceptCommands();
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("client disconnected");
        }
    }

    private void acceptCommands() throws IOException, InterruptedException {
        NetUtils.sendTcpMsg(toClient, QUERY_ASK.value, QUERY.value, USER_PROMP);
        TcpMessage msg = new TcpMessage(fromClient);
        while (!msg.message.endsWith("exit")) {
            if (validCredentials(msg.message)) sendRequest(msg.message.substring(hash.length()));
            else {
                NetUtils.sendTcpMsg(toClient, AUTH_FAIL.value, INIT.value, "credentials are not valid, bye bye ):");
                command_socket.close();
                data_socket.close();
                break;
            }
            NetUtils.sendTcpMsg(toClient, QUERY_ASK.value, QUERY.value, USER_PROMP);
            msg = new TcpMessage(fromClient);
        }
        NetUtils.sendTcpMsg(toClient, QUERY_EXIT.value, QUERY.value, "Exiting...");
    }

    private boolean validCredentials(String message) {
        return message.length() >= hash.length() && message.startsWith(hash);
    }

    public boolean authenticateUser(DataOutputStream toClient, DataInputStream fromClient) throws IOException {
        NetUtils.sendTcpMsg(toClient, AUTH_FAIL.value, INIT.value,
                "Enter your username & password with the from: username-password:");

        boolean success = false;
        TcpMessage msg = new TcpMessage(fromClient);
        String authInfo = msg.message;

        if (!authInfo.contains("-")) { // check if the format is correct
            NetUtils.sendTcpMsg(toClient, AUTH_FAIL.value, INIT.value, "User does not exist");
            command_socket.close();
            return false;
        }

        String username = authInfo.split("-")[0];
        String userPass = authInfo.split("-")[1];

        if (!userNames.contains(username)) {
            NetUtils.sendTcpMsg(toClient, AUTH_FAIL.value, INIT.value, "User does not exist");
            command_socket.close();
        } else if (!passwords.get(userNames.indexOf(username)).equals(userPass)) {
            for (int i = 0; i < 3; i++) {
                NetUtils.sendTcpMsg(toClient, AUTH_CHALLENGE.value, INIT.value, "re enter your password pls:");
                userPass = new TcpMessage(fromClient).message;
                if (passwords.get(userNames.indexOf(username)).equals(userPass)) {
                    success = true;
                    break;
                }
            }

            if (!success) {
                NetUtils.sendTcpMsg(toClient, AUTH_FAIL.value, INIT.value, "Incorrect password ):");
                command_socket.close();
            }
        } else
            success = true;

        if (success) {
            hash = (Math.abs((username + userPass + "66940").hashCode()) + "").substring(0, 8);
            NetUtils.sendTcpMsg(toClient, AUTH_SUCCESS.value, INIT.value, hash);
        }

        return success;
    }

    public void sendRequest(String url) throws IOException, InterruptedException {
        if (url.contains("planetary/apod")) getImage(url);
        else getSols(url);
    }

    private void getSols(String urlStr) throws IOException, InterruptedException {
        HttpResponse<String> response = NetUtils.httpCall(urlStr);
        JSONObject jsonResponse = new JSONObject(response.body());
        ArrayList<String> keys = new ArrayList<>();
        for (Object a : jsonResponse.getJSONArray("sol_keys").toList()) keys.add(a.toString());
        JSONObject sol = jsonResponse.getJSONObject(getRandomKey(keys));
        NetUtils.sendTcpJsonMsg(data_toClient, QUERY_SOL.value, QUERY.value, sol);
        NetUtils.sendTcpMsg(toClient, QUERY_SOL.value, QUERY.value, NetUtils.getJsonHash(sol));
    }

    private String getRandomKey(ArrayList<String> keys) {
        return keys.get(new Random().nextInt(keys.size()));
    }

    public void getImage(String urlStr) throws IOException, InterruptedException {
        HttpResponse<String> response = NetUtils.httpCall(urlStr);
        JSONObject responseObject = new JSONObject(response.body());

        if (responseObject.has("media_type") && responseObject.getString("media_type").contains("image")) {
            URL url = new URL(responseObject.getString("url"));
            BufferedImage image = ImageIO.read(url);
            NetUtils.sendTcpImageMsg(data_toClient, QUERY_IMAGE.value, QUERY.value, image);
            NetUtils.sendTcpMsg(toClient, QUERY_IMAGE.value, QUERY.value, NetUtils.getImageHash(image));
        } else {
            if (responseObject.has("code"))
                NetUtils.sendTcpMsg(toClient, QUERY_INFO.value, QUERY.value, responseObject.getString("msg"));
            else if (responseObject.has("media_type") && responseObject.getString("media_type").contains("video"))
                NetUtils.sendTcpMsg(toClient, QUERY_INFO.value, QUERY.value, "The returned file was not an image, it was a video!");
            else NetUtils.sendTcpMsg(toClient, QUERY_INFO.value, QUERY.value, "Something went wrong!!");
        }
    }
}