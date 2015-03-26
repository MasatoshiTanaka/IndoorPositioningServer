
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by ohyama on 2015/03/26.
 */
public class SocketServer {
    final static int PORT = 6666;
    public static void main(String[] args){
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            while(true) {
                try {
                    System.out.println("クライアントからの接続をポート6666で待ちます");
                    Socket socket = serverSocket.accept();
                    //ArrayList<String> arrayList = new ArrayList<String>();


                    // 受信
                    ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                    //arrayList = (ArrayList<String>)(objectInputStream.readObject());
                    Map<String,List<Integer>> wifidata = (Map<String,List<Integer>>)(objectInputStream.readObject());

                    for(String bssid : wifidata.keySet()){
                        List<Integer> rssiList = wifidata.get(bssid);
                        System.out.println(bssid + " " + rssiList);
                    }
                    // データ加工（ここでは頭に0を付与

                    /*
                    // 返信
                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    oos.writeObject(arrayList);
                    */

                    // ソケット削除
                    socket.close();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
