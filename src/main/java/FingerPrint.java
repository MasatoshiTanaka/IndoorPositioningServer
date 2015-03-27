/**
 * Created by ohyama on 2015/03/27.
 */

import sun.rmi.runtime.Log;

import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by ohyama on 2014/07/07.
 */
public class FingerPrint {

    public Map<String, Map<String, Map<Integer, BigDecimal>>> getNormallyDistributionFingerPrint(List<FundamentalStatics> fundamentalStaticsList){
        Map<String, Map<String, Map<Integer, BigDecimal>>> map = new TreeMap<>();
        for(FundamentalStatics fundamentalStatics : fundamentalStaticsList){
            if (fundamentalStatics.variance != Double.NaN && fundamentalStatics.variance != 0) {
                double width = (3 * fundamentalStatics.standardDeviation);
                for (double rssi = Math.ceil(fundamentalStatics.rssiAverage  - width); rssi <= fundamentalStatics.rssiAverage + width; rssi++) {
                    BigDecimal bd = normallyDistribution((int)rssi, fundamentalStatics.rssiAverage, fundamentalStatics.standardDeviation);
                    if(!map.containsKey(fundamentalStatics.placeID)){
                        Map<Integer, BigDecimal> map1 = new TreeMap<>();
                        Map<String, Map<Integer, BigDecimal>> map2 = new TreeMap<>();
                        map1.put((int)rssi, bd);
                        map2.put(fundamentalStatics.bssid, map1);
                        map.put(fundamentalStatics.placeID, map2);
                    }else{
                        if(!map.get(fundamentalStatics.placeID).containsKey(fundamentalStatics.bssid)){
                            Map<Integer, BigDecimal> map1 = new TreeMap<>();
                            map1.put((int)rssi, bd);
                            map.get(fundamentalStatics.placeID).put(fundamentalStatics.bssid, map1);
                        }else{
                            if(!map.get(fundamentalStatics.placeID).get(fundamentalStatics.bssid).containsKey((int)rssi)){
                                map.get(fundamentalStatics.placeID).get(fundamentalStatics.bssid).put((int)rssi, bd);
                            }else{
                                Logger logger = Logger.getLogger("");
                                logger.warning("");
                            }
                        }
                    }
                }
            }
        }
        return map;
    }

    public BigDecimal normallyDistribution(int number, double avg, double standardDeviation){
        BigDecimal bd = new BigDecimal(String.valueOf(((1 / (Math.sqrt(2 * Math.PI) * standardDeviation)) * Math.exp(-(Math.pow(number - avg, 2) / (2 * (standardDeviation * standardDeviation)))))));
        return bd;
    }
}

