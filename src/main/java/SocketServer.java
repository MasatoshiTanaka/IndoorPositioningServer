
import java.io.*;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by ohyama on 2015/03/26.
 */
public class SocketServer {
    final static int PORT = 6666;

    static String[] lgl22 = new String[]{
            "select count, placeID, bssid, rssi, channel from lgl22_0825_50_1",
            "select count, placeID, bssid, rssi, channel from lgl22_0825_50_2",
    };

    static String[] nexus2012 = new String[]{
            "select count, placeID, bssid, rssi, channel from nexus2012_0825_50_1",
            "select count, placeID, bssid, rssi, channel from nexus2012_0825_50_2",
    };

    static String[] nexus2013 = new String[]{
            "select count, placeID, bssid, rssi, channel from nexus2013_0825_50_1",
            "select count, placeID, bssid, rssi, channel from nexus2013_0825_50_2"
    };

    public static void main(String[] args) {
        try {
            System.out.println("クライアントからの接続をポート6666で待ちます");
            ServerSocket serverSocket = new ServerSocket(PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                try {
                    //ArrayList<String> arrayList = new ArrayList<String>();

                    // 受信
                    ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                    Map<String, List<Integer>> estimateWiFiData = (Map<String, List<Integer>>) (objectInputStream.readObject());

                    for (String bssid : estimateWiFiData.keySet()) {
                        List<Integer> rssiList = estimateWiFiData.get(bssid);
                        System.out.println(bssid + " " + rssiList);
                    }

                    Map<String, Map<String, List<Integer>>> fingerPrintWiFiData = new TreeMap();
                    Connection connection = DriverManager.getConnection("jdbc:mysql://localhost/wifidata", "root", "root");
                    PreparedStatement preparedStatement = connection.prepareStatement(nexus2012[1]);
                    ResultSet resultSet = preparedStatement.executeQuery();
                    while (resultSet.next()) {
                        String placeID = resultSet.getString("placeID");
                        int count = resultSet.getInt("count");
                        String bssid = resultSet.getString("bssid");
                        int rssi = resultSet.getInt("rssi");
                        int channel = resultSet.getInt("channel");
                        if (!fingerPrintWiFiData.containsKey(placeID)) {
                            Map<String, List<Integer>> map = new TreeMap();
                            List<Integer> list = new ArrayList(Arrays.asList(rssi));
                            map.put(bssid, list);
                            fingerPrintWiFiData.put(placeID, map);
                        } else {
                            if (!fingerPrintWiFiData.get(placeID).containsKey(bssid)) {
                                List<Integer> list = new ArrayList(Arrays.asList(rssi));
                                fingerPrintWiFiData.get(placeID).put(bssid, list);
                            } else {
                                fingerPrintWiFiData.get(placeID).get(bssid).add(rssi);
                            }
                        }
                    }

                    FundamentalStatics fundamentalStatics = new FundamentalStatics(fingerPrintWiFiData);
                    List<FundamentalStatics> fundamentalStaticsList = fundamentalStatics.getFundamentalStaticsList();
                    FingerPrint fingerPrint = new FingerPrint();
                    Map<String, Map<String, Map<Integer, BigDecimal>>> fingerPrintData = fingerPrint.getNormallyDistributionFingerPrint(fundamentalStaticsList);
                    List<Map.Entry> result = comparison(fingerPrintData, estimateWiFiData);
                    try {
                        System.out.println(result.get(0).getKey());
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(((String) result.get(0).getKey()).getBytes());
                        System.out.println(((String) result.get(0).getKey()));
                        outputStream.flush();
                    }catch(Exception e) {
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write("推定できません".getBytes());
                        System.out.println("推定できません");
                        outputStream.flush();
                    }
                    // ソケット削除
                    socket.close();
                } catch (Exception e) {

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<Map.Entry> comparison(Map<String, Map<String, Map<Integer, BigDecimal>>> fingerPrintData, Map<String, List<Integer>> estimateWiFiData){
        Map<String, BigDecimal> result = new TreeMap<>();
        for(String placeID : fingerPrintData.keySet()){
            for(String fingerPrintBssid : fingerPrintData.get(placeID).keySet()){
                for(int fingerPrintRssi : fingerPrintData.get(placeID).get(fingerPrintBssid).keySet()){
                    BigDecimal weight = fingerPrintData.get(placeID).get(fingerPrintBssid).get(fingerPrintRssi);
                    for(String estimateBssid : estimateWiFiData.keySet()){
                        for(int estimateRssi : estimateWiFiData.get(estimateBssid)){
                            if(fingerPrintBssid.equals(estimateBssid) && fingerPrintRssi == estimateRssi){
                                if(!result.containsKey(placeID)){
                                    result.put(placeID, weight);
                                }else{
                                    result.put(placeID, result.get(placeID).add(weight));
                                }
                            }
                        }
                    }
                }
            }
        }
        List<Map.Entry> entries = new ArrayList<>(result.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry>() {
            @Override
            public int compare(Map.Entry o1, Map.Entry o2) {
                return ((BigDecimal)o2.getValue()).compareTo((BigDecimal)o1.getValue());
            }
        });
        return entries;
    }
}
