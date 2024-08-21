package travel.entity;

import lombok.Data;

import java.util.Map;

/**
 * @author fdse
 */
@Data
public class TravelResult {
    private boolean status;

    private double percent;

    private TrainType trainType;

    private Map<String, String> prices;

    private String message;

    public TravelResult() {
        //Default Constructor
    }
}
