package travelplan.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;

/**
 * @author fdse
 */
@Data
@AllArgsConstructor
public class RoutePlanResultUnit {
    private String tripId;

    private String trainTypeId;

    private String fromStationName;

    private String toStationName;

    private ArrayList<String> stopStations;

    private String priceForSecondClassSeat;

    private String priceForFirstClassSeat;

    private Date startingTime;

    private Date endTime;

    public RoutePlanResultUnit() {
        //Default Constructor
    }
}
