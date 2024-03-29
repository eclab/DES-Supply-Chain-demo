package  edu.rutgers.sc3;


import java.util.Vector;
import java.io.*;

import sim.engine.*;
import sim.util.*;
import sim.util.distribution.*;
import sim.des.*;
import sim.des.portrayal.*;

import edu.rutgers.util.*;
import edu.rutgers.supply.*;
import edu.rutgers.supply.Disruptions.Disruption;

/** A ThrottleQueue is normally used in tandem with a
    SimpleDelay. Taken together, they model a production
    stage where only 1 batch of the material can be processed
    at a given time, and not-yet-processed material is waiting
    for its turn outside of the "work area". (The ThrottleQueue 
    represents the waiting area, and the SimpleDelay, the
    work area). This can model, for example, a bread oven
    that can bake one batch of loaves at a time, or a forklift
    that can move one pallet of stuff between warehouses.

    <P>Each batch can have its own processing time (drawn from
    a random distribution), but, since only one batch is processed
    at a time, we can model the "work area" with a SimpleDelay
    (modifying its delay time before processing each batch), rather
    than a Delay. This is supposed to be more computationally
    efficient.
   
    <P>In the ThrottleQueue-SimpleDelay tandem, the ThrottleQueue
    controls the behavior of the SimpleDelay, to ensure that it holds
    exactly one batch at a time, and the processing time of each batch
    is drawn from a specified distribution. The ThrottleQueue is
    attached in front of the SimpleDelay it controls.

    <p>The ThrottleQueue sets the capacity of the SimpleDelay to 1,
    and sets itself as the slackProvider for the delay, so that every
    time the delay is finished with a batch ("the bread has been
    baked") it pulls one more batch ("a batch of raw loaves") from the
    queue, via the mkBatch() mechanism.  Additionally, when something
    is put into the Queue, its offerReceiver checks whether the delay
    is empty ("the oven is idle"), and if it is, it offers resource to
    the delay. These two mechanisms, in combination, ensure that the
    delay ("the oven") is never idle, as long as there is anything to
    be processed.
 */
public class ThrottleQueue extends sim.des.Queue    implements     Named
{

    /** Part of which production unit am I? This is needed in GraphAnalysis, as well as for auto-reloading */
    private 	 Production whose=null;
    void setWhose(  Production  _whose) {whose = _whose;}
    Production 	getWhose() { return whose; }

    
    /** A ProdDelay object (which in SC-3 is designed as a Delay class,
	for compatibility with other uses), which uses as if it was just 
	a capacity-1 SimpleDelay for which the ThrottleQueue
	serves as an input buffer.   */
    final private Delay delay;

/** Creates a Queue to be attached in front of the Delay, and links it
    up appropriately. This is done so that we can model a production
    facility (or a transportation service, etc) which can handle no
    more than a given number of batches at any given time.

    <p>Created by this constructor is a Queue object into which one
    can put any number of "raw" batches, where they will sit and wait
    for the production facility to grab them whenever it's ready.

    @param _delay Models the production step whose capacity we want to restrict. (For example, a bread oven with space for exactly 1 batch of loaves, or a truck that has space for exactly 1 shipping container of stuff).

    @param cap The max number of batches that the production unit (the Delay object) can physically handle simultaneously. (Usually, 1).

     */
    public  ThrottleQueue(Delay _delay, double cap)
{
	super(_delay.getState(), _delay.getTypicalProvided());
	delay = _delay;
	setOffersImmediately(true);
	setName("TQ for " + delay.getName());
	if (delay.getTypicalProvided() instanceof Entity) {
	    if (cap!=1) throw new IllegalArgumentException("Entity-based ThrottleQueue must have cap=1 for its delay"); 
	} 
	    
	
	delay.setCapacity(cap);
	
	addReceiver(delay);
	//-- this setting ensures that when the delay becomes empty, it
	//-- will call this ThrottlQueue's offerReceiver, to be refilled
	delay.setSlackProvider(this);	
    }

    //void setDelayDistribution(AbstractDistribution d) {
    //delayDistribution = d;
    //}
    
    /** This method is called whenever a batch is put into this
	ThrottleQueue (due to the immediateOffers flag), and whenever
	the throttled SimpleDelay becomes empty (through the
	slackProvider mechanism). When it is called, it verifies that
	the throttled SimpleDelay is in fact empty, and if it is, sets
	a new randomly picked delay time, and then puts a batch into
	the delay.

	@param receiver This is expected to be the ProdDelay controlled
	by this ThrottleQueue
    */
    protected boolean offerReceiver(Receiver receiver, double atMost) {
	if (receiver != delay)  throw new IllegalArgumentException("Wrong receiver for ThrottleQueue");

	/*
	if (!(delay.getTypicalProvided() instanceof Entity) && (atMost!=delay.getCapacity())) {
	    throw new IllegalArgumentException("Wrong batch size (given="+
					       receiver.getAmount()+", expected=" +
					       delay.getCapacity());
	}
	*/

	if (delay.getDelayed() > 0) return false; // the SimpleDelay is not empty

	double now = getState().schedule.getTime();

	if (!whose.isOpenForBusiness(now)) return false; // Halt disruption in effect

			    
	//double t = state.schedule.getTime();       

	boolean z=super.offerReceiver(receiver, atMost);
			    
	return z;
    }



    
    /** How many batches are there waiting for processing, and
	how many are waiting for processing */
    public String hasBatches() {
	String s = "" + (long)getAvailable();
	if (delay instanceof Reporting.HasBatches) {
	    s += "+(" + ((Reporting.HasBatches)delay).hasBatches() +")";
	}
	return s;
    }

    public void step(SimState state) throws IllegalArgumentException {
	super.step(state);
    }

    /** Just for debugging */
    /*
    public boolean accept(Provider provider, Resource r, double atLeast, double atMost) {
	boolean z = super.accept( provider, r, atLeast, atMost);
	if (Demo.verbose) {
	    if (r instanceof Batch) {
		double t = state.schedule.getTime();       		
		((Batch)r).addToMsg("["+getName()+".acc@"+t+", hb="+hasBatches()+"]");
	    }
	}
	return z;
    }
    */

    
    /** Just for debugging */
    /*
    public boolean offerReceivers() {
	double t = state.schedule.getTime();
	System.out.println("At " + t +", "+getName() + " doing offerReceivers(), had=" + hasBatches());
	boolean z = super. offerReceivers();
	System.out.println("At " + t +", "+getName() + " done offerReceivers(). Result=" + z +"; has=" + hasBatches());
	return z;
 	
    }
    */

    /** Simply exposes offerReceivers() to classes in this package. Can be
	used to "prime the system". */
    protected boolean offerReceivers() {
	return super.offerReceivers();
    }

    /** If true, this indicates that whenever this queue is called
	upon to provide a batch for its downstream receiver (likely,
	via that receiver slackProvider mechanism) it will make itself
	non-empty, if at all possible. */
    boolean autoReloading = false;
    void setAutoReloading(boolean x) {
	 autoReloading = x;
	 if (x && whose==null) {
	     throw new IllegalArgumentException("Must set 'whose' before enabling auto-reloading");
	 }
    }
    

    /** This method ensures that whenever this queue is called
	upon to provide a batch for the ProdDelay (via its slackProvider
	mechanism), it will make itself non-empty, if at all possible.

	<p>The name has been changed from provide() to offer() because of the
	change in the DES API (see the slack call in SimpleDelay) between
	2022 and 2023.
    */
    public boolean provide(Receiver receiver) {
	//public boolean offer(Receiver receiver) {
	double now = state.schedule.getTime();
	if (Demo.verbose) System.out.println("At t="+now+", " +getName() + ".provide()");
	if (getAvailable()==0 && autoReloading) {
	    if (Demo.verbose) System.out.println(getName() + " call mkBatch");
	    whose.mkBatch();
	}
	if (getAvailable()==0) return false;
	//return super.offer(receiver);
	return super.provide(receiver);
    }

    public double getEverReleased()  {
	if (delay instanceof Reporting.HasBatches) {
	    return ((Reporting.HasBatches)delay).getEverReleased();
	} else {
	    throw new UnsupportedOperationException();
	}
    }
    
}
     
