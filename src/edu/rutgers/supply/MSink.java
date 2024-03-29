package  edu.rutgers.supply;

import java.util.*;
import java.text.*;

import sim.engine.*;
import sim.util.*;
import sim.util.distribution.*;
import sim.des.*;

/** A Metered Sink is a wrapper around Sink, designed to keep track of
    the amount of resource it has consumed, and to print some
    debugging information if needed. It can work both with fungible resources
    (CountableResource) and with "palletized" ones (Batch).
 */
public class MSink extends Sink implements Reporting {

    /** How much stuff (units) has this Sink consumed since it's been created? */    
    public double getEverConsumed() { return everConsumed; }
    protected double everConsumed = 0;

    /** How many Batches has this sink ever consumed? This is relevant
	if the sink receives a Batch resource.
     */
    public double getEverConsumedBatches() { return everConsumedBatches; }
    protected double everConsumedBatches = 0;

    /** How much stuf was consumed in the most recent accept()
        call? (In terms of underlying CountableResource) */
    public double getLastConsumed() { return lastConsumed; }
    protected double lastConsumed = 0;

    public MSink(SimState state, Resource typical) {
	super(state,typical);
	String name = "Sink." + typical.getName();
	setName(name);
    }
    
    public boolean accept(Provider provider, Resource resource, double atLeast, double atMost) {

	boolean z;
	double consumed = 0;
	if (resource instanceof CountableResource) {
	    CountableResource cr = (CountableResource) resource;
	    double amt0 = cr.getAmount();
	    
	    z =super.accept( provider,  resource,  atLeast,  atMost);
	    double amt1 = cr.getAmount();
	    //if (Demo.verbose) System.out.println("Sink.accept(" + getName() + ","+amt0+"/"+amt1+","+ atLeast+","+ atMost +")");
	    consumed = amt0 - amt1;

	} else if (resource instanceof Batch) {
	    Batch b = (Batch) resource;
	    double  w = b.getContentAmount();

	    z =super.accept( provider,  resource,  atLeast,  atMost);
	    consumed = z? w : 0;
	    everConsumedBatches ++;
	} else throw new IllegalArgumentException("Resource class not supported: " + resource);
	lastConsumed = consumed;
	everConsumed += consumed;
	return z;
    }

    public String report() {	
	String s  = "[" + getName() + " has consumed " + everConsumed + " u";
	if (everConsumedBatches>0) s += " = " + 	everConsumedBatches + " ba";
	s +=  "]";
	return s;
    }


    
}
    
