
import com.mysql.jdbc.log.Log;

import java.io.*;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
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
            "select count, placeID, bssid, rssi, freq from lgl22_1",
            "select count, placeID, bssid, rssi, freq from lgl22_2"
    };

    static String[] nexus2012 = new String[]{
            "select count, placeID, bssid, rssi, freq from nexus2012_1",
            "select count, placeID, bssid, rssi, freq from nexus2012_2"
    };

    static String[] nexus2013 = new String[]{
            "select count, placeID, bssid, rssi, freq from nexus2013_1",
            "select count, placeID, bssid, rssi, freq from nexus2013_2"
    };

    public static void main(String[] args)  {
        Logger logger = Logger.getLogger("");
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            Map<String, Map<String, Map<Integer, BigDecimal>>> fingerPrint = createFingerPrint();
            while (true) {
                Socket socket = serverSocket.accept();
                ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                Map<String, List<Integer>> estimateWiFiData = (Map<String, List<Integer>>) (objectInputStream.readObject());

                List<Map.Entry> result = comparison(fingerPrint, estimateWiFiData);

                try {
                    System.out.println(result.get(0).getKey());
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(((String) result.get(0).getKey()).getBytes());
                    outputStream.flush();
                    logger.info(((String) result.get(0).getKey()));
                } catch (Exception e) {
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write("推定できませんでした".getBytes());
                    outputStream.flush();
                    logger.info("推定できませんでした");
                }
                socket.close();
            }
        } catch (ClassNotFoundException e) {
            logger.info("ClassNotFoundException");
            e.printStackTrace();
        } catch (IOException e) {
            logger.info("IOException");
            e.printStackTrace();
        }
    }

    public static Map<String, Map<String, Map<Integer, BigDecimal>>> createFingerPrint(){
        Logger logger = Logger.getLogger(" createFingerPrint");
        logger.info("基準データ作成開始");
        Map<String, Map<String, Map<Integer, BigDecimal>>> fingerPrintData = null;
        try {
            Map<String, Map<String, List<Integer>>> fingerPrintWiFiData = new TreeMap();
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost/wifidata", "root", "root");
            PreparedStatement preparedStatement = connection.prepareStatement(lgl22[0]);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                String placeID = resultSet.getString("placeID");
                int count = resultSet.getInt("count");
                String bssid = resultSet.getString("bssid");
                int rssi = resultSet.getInt("rssi");
                int freq = resultSet.getInt("freq");
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
            fingerPrintData = fingerPrint.getNormallyDistributionFingerPrint(fundamentalStaticsList);
            logger.info("基準データ作成成功");
        } catch (SQLException e) {
            logger.warning("基準データ作成に失敗しました");
            System.exit(0);
        }
        return fingerPrintData;
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
