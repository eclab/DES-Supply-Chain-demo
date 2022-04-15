package  edu.rutgers.pharma3;

import java.util.*;
import java.text.*;

import sim.engine.*;
import sim.util.*;
import sim.util.distribution.*;
//import sim.field.continuous.*;
import sim.des.*;

import edu.rutgers.util.*;

/** A Lot object contains useful information about a lot, such as the
    manufacturing date and expiration date. */

public class Lot {
    /** Maps lotNo to the Lot object */
    HashMap<Long, Lot> allLots = new HashMap<>();

    final long lotNo;

    final double manufacturingDate;
    /** The expiration date of this lot. If the product never expire,
	we store Double.POSITIVE_INFINITY here */
    final double expirationDate;

    private Lot(long _lotNo, double now, double _expirationDate) {
	lotNo = _lotNo;
	manufacturingDate = now;
	expirationDate = _expirationDate;
	
    }
    
    static void registerLot(long lotNo, double now, double _expirationDate) {
	Lot x = new Lot( lotNo, now,  _expirationDate);
	allLots.put( new Double(lotNo), x);
    }
    
}
