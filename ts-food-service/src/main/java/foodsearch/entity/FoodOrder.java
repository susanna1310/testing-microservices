package foodsearch.entity;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Document(collection = "foodorder")
@JsonIgnoreProperties(ignoreUnknown = true)
public class FoodOrder
{
    @Id
    private UUID id;

    private UUID orderId;

    //1:train food;2:food store
    private int foodType;

    private String stationName;

    private String storeName;

    private String foodName;

    private double price;

    public FoodOrder()
    {
        //Default Constructor
    }
}
