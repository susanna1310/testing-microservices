package inside_payment.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author fdse
 */
@Data
@AllArgsConstructor
public class PaymentInfo
{
    private String userId;

    private String orderId;

    private String tripId;

    private String price;

    public PaymentInfo()
    {
        //Default Constructor
    }
}
