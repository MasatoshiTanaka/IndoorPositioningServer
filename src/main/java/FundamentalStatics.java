/**
 * Created by ohyama on 2015/03/27.
 */

import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by ohyama on 2015/01/05.
 */
public class FundamentalStatics {
    Map<String, Map<String, List<Integer>>> wifiData;
    String placeID;
    String bssid;
    double rssiAverage;
    double deviation;
    double variance;
    double standardDeviation;

    FundamentalStatics(Map<String, Map<String, List<Integer>>> wifiData){
        this.wifiData = wifiData;
    }

    FundamentalStatics(String placeID, String bssid, double rssiAverage ,double deviation, double variance, double standardDeviation){
        this.placeID = placeID;
        this.bssid = bssid;
        this.rssiAverage = rssiAverage;
        this.deviation = deviation;
        this.variance = variance;
        this.standardDeviation = standardDeviation;
    }

    public List<FundamentalStatics> getFundamentalStaticsList(){
        List<FundamentalStatics> fundamentalStaticsList = new ArrayList<>();
        for (String place : wifiData.keySet()) {
            for (String bssid : wifiData.get(place).keySet()) {
                IntSummaryStatistics intSummaryStatistics = wifiData.get(place).get(bssid).stream().collect(Collectors.summarizingInt(x -> x));
                long size = intSummaryStatistics.getCount();
                double rssiAve = intSummaryStatistics.getAverage();
                double deviation = 0;
                for (int rssi : wifiData.get(place).get(bssid)) {
                    deviation += Math.pow((rssi - rssiAve), 2);
                }
                double variance = deviation / size;
                double standardDeviation = Math.sqrt(variance);
                fundamentalStaticsList.add(new FundamentalStatics(place, bssid,  rssiAve ,deviation, variance, standardDeviation));
            }
        }
        return fundamentalStaticsList;
    }
}
