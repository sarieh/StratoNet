import org.json.JSONObject;
import utils.*;

import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

import static utils.MsgPhase.*;
import static utils.MsgType.*;

public class StratoNetClient {

    private String hash;
    private static final String BASE_URL = "https://api.nasa.gov/";
    private static final String key = "api_key=x4CQROO9U4j0qdiOACP5ocKoEir7eHK3X6d2yO7v";
    private static final String api1 = "planetary/apod";
    private static final String api2 = "insight_weather/";
    PrintWriter out;

    public static void main(String[] args) {
        StratoNetClient client = new StratoNetClient();
        client.start();
    }

    public void start() {
        try {
            InetAddress host = InetAddress.getByName("localhost");
            Socket command_socket = new Socket(host, NetUtils.WELCOMING_AUTH_PORT);
            Socket data_socket = new Socket(host, NetUtils.WELCOMING_DATA_PORT);

            DataInputStream fromServer = new DataInputStream(command_socket.getInputStream());
            DataOutputStream toServer = new DataOutputStream(command_socket.getOutputStream());
            DataInputStream data_fromServer = new DataInputStream(data_socket.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            out = new PrintWriter(command_socket.getOutputStream(), true);

            System.out.println("Server: " + new TcpMessage(fromServer).message);
            NetUtils.sendTcpMsg(toServer, AUTH_REQUEST.value, INIT.value, br.readLine());
            while (true) {
                TcpMessage msg = new TcpMessage(fromServer);
                if (msg.phase == INIT.value) { //phase 1
                    if (msg.type == AUTH_FAIL.value) {
                        System.out.println("Server: " + msg.message);
                        break;
                    } else if (msg.type == AUTH_CHALLENGE.value) {
                        System.out.println("Server: " + msg.message);
                        NetUtils.sendTcpMsg(toServer, AUTH_REQUEST.value, INIT.value, br.readLine());
                    } else if (msg.type == AUTH_SUCCESS.value) { //AUTH_SUCCESS
                        System.out.println("successfully logged in (:");
                        hash = msg.message;
                    } else System.err.println("error");
                }

                if (msg.phase == QUERY.value) { //phase 2
                    if (msg.type == QUERY_EXIT.value) {
                        System.out.println("Server: " + msg.message);
                        break;
                    } else if (msg.type == QUERY_IMAGE.value) {
                        TcpDataMessage dataMsg = new TcpDataMessage(data_fromServer);
                        if (dataMsg.validHash(msg.message)) displayImage(dataMsg.data);
                        else System.out.println("credentials were not correct, not safe to display the image");
                    } else if (msg.type == QUERY_ASK.value) {
                        System.out.println("Server: " + msg.message);
                        NetUtils.sendTcpMsg(toServer, AUTH_SUCCESS.value, QUERY.value, hash + getApiRequest(br.readLine()));
                    } else if (msg.type == QUERY_SOL.value) {
                        TcpDataMessage dataMsg = new TcpDataMessage(data_fromServer);
                        JSONObject sol = null;
                        if (dataMsg.validHash(msg.message)) sol = new JSONObject(new String(dataMsg.data));
                        else System.out.println("credentials were not correct, not safe to display the image");
                        if (sol != null) displaySolInfo(sol);
                    } else if (msg.type == QUERY_INFO.value) System.err.println("Server: " + msg.message);
                    else System.err.println("error?!");
                }
            }

            toServer.close();
            fromServer.close();
            command_socket.close();
            data_socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void displaySolInfo(JSONObject sol) {
        StringBuilder bld = new StringBuilder();

        JSONObject press = sol.getJSONObject("PRE");
        String season = sol.getString("Season");
        String monthOrdinal = sol.getInt("Month_ordinal") + "";
        String First_UTC = sol.getString("First_UTC");
        String Last_UTC = sol.getString("Last_UTC");
        String av = press.getFloat("av") + "";
        String mn = press.getFloat("mn") + "";
        String mx = press.getFloat("mx") + "";

        bld.append("Here are the information of the returned sol:\n")
                .append("\tin the ").append(season).append("/n")
                .append("\t\tmaximum pressure: ").append(mx).append("\n")
                .append("\t\tminimum pressure: ").append(mn).append("\n")
                .append("\t\taverage pressure: ").append(av).append("\n")
                .append("\twith a \"Month ordinal\" of: ").append(monthOrdinal).append("\n")
                .append("\tFirst UTC is: ").append(First_UTC).append("\n")
                .append("\tLast UTC is: ").append(Last_UTC).append("\n");

        System.out.println(bld.toString());
    }

    private void displayImage(byte[] image) {
        JFrame frame = new JFrame();
        frame.getContentPane().add(new JLabel(new ImageIcon(image)));
        frame.pack();
        frame.setVisible(true);
    }

    private String getApiRequest(String input) {
        String[] inLst = input.split(" ");
        if (inLst[0].equals("1")) return BASE_URL + api1 + "?" + key + (inLst.length > 1 ? "&date=" + inLst[1] : "");
        else if (input.split(" ")[0].equals("2")) return BASE_URL + api2 + "?" + key + "&feedtype=json&ver=1.0";
        else return "exit";
    }
}
