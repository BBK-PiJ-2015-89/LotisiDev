package testapp.silencertestapp;

/**
 * Created by graemewilkinson on 14/08/2017.
 */

public class Condition {

    private String wifiNetwork;
    private int startTime;
    private int endTime;

    public Condition(String wifiNetwork, int startTime, int endTime){
        this.wifiNetwork = wifiNetwork;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getWifiNetwork(){
        return wifiNetwork;
    }

    public int getStartTime(){
        return startTime;
    }

    public int getEndTime (){
        return endTime;
    }

}
