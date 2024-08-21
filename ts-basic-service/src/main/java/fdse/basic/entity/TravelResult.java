package fdse.basic.entity;

import java.util.Map;

import lombok.Data;

/**
 * @author fdse
 */
@Data
public class TravelResult
{
    private boolean status;

    private double percent;

    private TrainType trainType;

    private Map<String, String> prices;

    public TravelResult()
    {
        //Default Constructor
    }

    public boolean isStatus()
    {
        return status;
    }
}
