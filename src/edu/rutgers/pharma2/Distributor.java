package  edu.rutgers.pharma2;

import java.util.*;
import java.text.*;

import sim.engine.*;
import sim.util.*;
import sim.util.distribution.*;
//import sim.field.continuous.*;
import sim.des.*;

import edu.rutgers.util.*;

/** The main Queue is the storage facility; additionally a Delay is used to ship things out */
public class Distributor extends sim.des.Queue
    implements Reporting //,	       Steppable, Named
{

    int interval;
    double batchSize;
    
    Delay shipOutDelay;
    
    Distributor(SimState _state, String name, Config config,
		      CountableResource resource) throws IllegalInputException {
	super(_state, resource);	
	setName(name);
	ParaSet para = config.get(name);
	if (para==null) throw new  IllegalInputException("No config parameters specified for element named '" + name +"'");

	batchSize = para.getDouble("batch");
	interval = (int)para.getLong("interval");

	shipOutDelay = new Delay( getState(),  resource);
	shipOutDelay.setDelayDistribution(para.getDistribution("shipOutDelay",getState().random));				       

    }

    //Receiver rcv;

    double needsToShip=0, everShipped=0;
    public double getNeedsToShip() { return needsToShip; }
    public double getEverShipped() { return everShipped; }

    /** To whom are we shipping */
    void setDeliveryReceiver(Receiver rcv) {
	shipOutDelay.addReceiver(rcv);
    }

    /** Adds x to the total amount of stuff that we need to ship eventually */
    void addToPlan(double x)
    {
	needsToShip += x;	
    }
    

    private double lastShippedAt = 0, lastMonthShippedAt=0;
    private int loadsShipped = 0;
    public int getLoadsShipped() { return loadsShipped; }

    /** Ships product out on a certain schedule */
    public void step​(sim.engine.SimState _state) {

	double t = getState().schedule.getTime();
	double month = Math.floor(t/interval);

	//-- How many batches can we form?
	double stillNeeded =  needsToShip - everShipped;
	
	if ( (month> lastMonthShippedAt) && stillNeeded>0) {	

	    double onHand = Math.min( getAvailable(), stillNeeded);

	    double shouldShip = 0;
	    if (onHand == stillNeeded) {
		// We can fulfill the entire demand, even if an odd lot needs to be generated
		shouldShip = onHand;
	    } else {
		int nb = (int)Math.floor( onHand / batchSize);
		shouldShip = nb * batchSize;
	    }
	    
	    if (shouldShip>0) {
		if (Demo.verbose) System.out.println("At t=" + getState().schedule.getTime() + ", Distro has " +  getAvailable() +", stillNeeded="+stillNeeded +
				   ". Shipping " +  shouldShip);
	
		offerReceiver( shipOutDelay, shouldShip);

		lastShippedAt = t;
		lastMonthShippedAt=month;
		everShipped += shouldShip;
		loadsShipped++;

	    }
	}
    }

    public double getOnHand() {
	return getAvailable();
    }
    
   public String report() {
       String s = "Shipping plan=" + needsToShip +", has shipped=" + everShipped + ", in " + loadsShipped+ " loads. Of this, " + shipOutDelay.getDelayed() + " is still being shipped. Remains on hand=" + getAvailable();
       return wrap(s);
    }

    
}
