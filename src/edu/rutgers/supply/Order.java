package  edu.rutgers.supply;

import java.util.*;


/** Info about a single order, typically one sent from one pool to another.
    In SC2, lists of Order objects are kept by senders and receivers, so
    that the former can keep track of what they need to do, and the latter
    can keep track of what they expect to receive. This is essential
    for managing order expiration on the receiver end (when an ordered
    shipment never arrives, e.g. due to some disruption).
 */
public class Order implements Cloneable {

    /** Used to generate globally unique order ID numbers */
    private static long lastId = 0;

    /** A globally unique ID number of this order */
    final public long id;
    /** When this order was placed */
    final public double date;
    /** This is set when the order is closed; sometimes used for statistics */
    public double filledDate=Double.NaN;
    /** The original order size (before any reduction due to filling) */
    final public double amount0;
    /** Order size. This can be changed in the sender's copy of the order as the order is being partially filled */
    public double amount;

    /** Info about the Pool that needs to send the product and the
	Receiver (maybe a shipping Delay etc) that will receive it)
     */
    public final Channel channel;
    
    public Order(double _date, Channel _channel, double _amount) {
	date = _date;
	amount0 = amount = _amount;
	channel = _channel;
	id = (lastId++);
    }

    /** Makes a shallow copy of this Order. This is all we need, as
	the only reason to have a copy is so that we can modify the
	amount separately in the sender's and receiver's copies of the
	order.
    */
    public Order copy() {
	try {
	    return (Order)super.clone();
	} catch ( CloneNotSupportedException ex) {
	    throw new AssertionError();
	}
    }

    public String toString() {
	return "(" + channel.name+ "," + date + " to "+filledDate+"," + amount + " of " + amount0+")";
    }

    public void filled(double now) {
	filledDate = now;
    }
}
