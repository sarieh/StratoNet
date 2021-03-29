import utils.NetUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class StratoNetServer {
    static ArrayList<String> userNames = new ArrayList<>();
    static ArrayList<String> passwords = new ArrayList<>();

    public static void main(String[] args) {
        try {
            BufferedReader reader;
            reader = new BufferedReader(new FileReader(System.getProperty("user.dir") + "/users.txt"));
            String line = reader.readLine();
            while (line != null) {
                String[] userInfo = line.split("-");
                userNames.add(userInfo[0]);
                passwords.add(userInfo[1]);
                System.out.println(line);
                line = reader.readLine();
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        StratoNetServer srv = new StratoNetServer();
        srv.start();
    }

    private void start() {
        try {
            ServerSocket serverSocket_data = new ServerSocket(NetUtils.WELCOMING_DATA_PORT);
            ServerSocket serverSocket_command = new ServerSocket(NetUtils.WELCOMING_AUTH_PORT);
            while (true) {
                Socket command_socket = serverSocket_command.accept();
                new ClientThread(command_socket, serverSocket_data, userNames, passwords).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}


