package fdse.basic.entity;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author fdse
 */
@Data
@AllArgsConstructor
public class PriceConfig
{
    private UUID id;

    private String trainType;

    private String routeId;

    private double basicPriceRate;

    private double firstClassPriceRate;

    public PriceConfig()
    {
        //Empty Constructor
    }
}
