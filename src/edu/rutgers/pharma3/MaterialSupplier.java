package  edu.rutgers.pharma3;

import java.util.Vector;
import java.text.*;

import sim.engine.*;
import sim.util.*;
import sim.util.distribution.*;
import sim.des.*;

import edu.rutgers.util.*;

/** Models a facility that supplies a raw material, plus the pharma co's 
    quality testing stage that handles the material from this supplier.

    <p>The Receiever (Sink) feature of this class is used so that we
    can use it "accept" batches coming out of the 3 stages of delays
    (prod, transportation, QA).

    <p>A MaterialSupplier does not need to be scheduled. Rather, a controlling
    element (company headquearters) needs to do 2 things:
<ol>

<li>At the beginning, call setQaReceiver(Receiver rcv), to specify the
object to which this supplier should push QA'ed material.

<li>Call receiveOrder(double amt) whenever it wants to it order some
 amount of the material. Internally, this will push some stuff into a
 delay, which will later trigger rcv.accept() on the downstream
 receiever. </ol>

 */
public class MaterialSupplier //extends Sink 
    implements  //Receiver,
			    Named,   Reporting
{

    private double outstandingOrderAmount=0;
    private double everOrdered=0;
    //private double badResource = 0, releasedGoodResource=0;

    //    private long everOrderedBatches=0;
    //private long badResourceBatches = 0, releasedGoodResourceBatches=0;
    private long startedProdBatches = 0;
    
    
    public double getOutstandingOrderAmount() { return outstandingOrderAmount; }
    public double getEverOrdered() { return everOrdered; }
    //public double getBadResource() { return badResource; }
    //public double getReleasedGoodResource() { return releasedGoodResource; }
 
 
    /** Production delay */
    private final Delay prodDelay;
    /** Transportation delay */
    private final Delay transDelay;
    private final QaDelay qaDelay;

    private final sim.des.Queue needProd, needTrans, needQa;

        
    /** Similar to "typical", but with the storage array */
    private final Resource prototype;
    Resource getPrototype() { return prototype; }

    /** Creates a MaterialSupplier that will supply either a specified
	CountableResource or "batched resource" based on that resource.
	@param needLots If true, the supplier will supply lots 
     */
    static MaterialSupplier mkMaterialSupplier(SimState state, String name, Config config, 
					CountableResource resource, boolean needLots) 	throws IllegalInputException {
	ParaSet para = config.get(name);
	if (para==null) throw new  IllegalInputException("No config parameters specified for element named '" + name +"'");
	Resource proto = 
	    needLots?  Batch.mkPrototype(resource, para):
	    resource;
	return new   MaterialSupplier( state, para, proto);
    }
	
    protected SimState state;

    /** Creates a Queue in front of the Delay, and links it up
	appropriately
     */
    private sim.des.Queue controlInput(Delay delay, double cap) {
	sim.des.Queue need = new sim.des.Queue(state, prototype);
	need.setOffersImmediately(true);
	delay.setCapacity(cap);

	need.addReceiver(delay);
	delay.setSlackProvider(need);
	
	return need;
    }
    
    /** @param resource The product supplied by this supplier. Either a "prototype" Batch, or a CountableResource.
     */
    private MaterialSupplier(SimState _state, ParaSet para,
			     Resource resource	     ) 	throws IllegalInputException {
	//super(state, resource);
	state = _state;
	prototype = resource;

	setName(para.name);

	standardBatchSize = para.getDouble("batch");

	double cap = (prototype instanceof Batch) ? 1:    standardBatchSize;	

	prodDelay = new Delay(state, resource);
	prodDelay.setDelayDistribution(para.getDistribution("prodDelay",state.random));
	needProd = controlInput(prodDelay, cap);
	
	
	transDelay = new Delay(state, resource);
	transDelay.setDelayDistribution(para.getDistribution("transDelay",state.random));
	needTrans = controlInput(transDelay, cap);
	
	qaDelay = QaDelay.mkQaDelay( para, state, resource);
	needQa = controlInput(qaDelay, cap);
		
	//--- link them all
	prodDelay.addReceiver( needTrans );
	transDelay.addReceiver( needQa );
	//-- the output for qaDelay will be added by setQaReceiver

	
    }

    final double standardBatchSize;

    /** The place to which good stuff goes after QA, e.g. a manufacturing
	plant.
    */
    // FIXME: an array of receivers can be used instead, with its own
    private Receiver rcv;
    
    /** Sets the destination for the product that has passed the QA. This
	should be called after the constructor has returned, and before
	the simulation starts.
       @param _rcv The place to which good stuff goes after QA
     */
    void setQaReceiver(Receiver _rcv) {
	rcv = _rcv;
	qaDelay.addReceiver( rcv );
    }

    
    /** This method is called by an external customer when it needs
	this supplier to send out some amount of stuff. It "loads some
	stuff on a ship", i.e. puts it into the delay.
    */
    void receiveOrder(double amt) {
	outstandingOrderAmount += amt;
	everOrdered += amt;
	startAllProduction();
    }

    /** Initiate the production process on as many batches as needed.
	FIXME: we always use the standard batch size, and don't use 
	the last short batch, in order to simplify Production.
    */
    private boolean startAllProduction() {
	int bcnt = 0;
	double x = standardBatchSize;
	double t = state.schedule.getTime();
	while (outstandingOrderAmount>0) {
	//double x = Math.min(outstandingOrderAmount ,  standardBatchSize);

	    Resource batch = (prototype instanceof Batch) ? ((Batch)prototype).mkNewLot(x, t) :
	    new CountableResource((CountableResource)prototype, x);

	    Provider provider = null;
	    if (Demo.verbose) System.out.println( "At t=" + t+", " + getName() + " putting batch of "+x+" into needProd, bcnt=" + bcnt +", outstandingOrderAmount=" + outstandingOrderAmount);

	    double a =batch.getAmount();

	    double y = (batch instanceof Batch)? ((Batch)batch).getContentAmount() : ((CountableResource)batch).getAmount();

	    boolean z = needProd.accept( provider, batch, a, a);

	    if (!z) throw new AssertionError("needProd is supposed to accept everything, but it didn't!");

	    outstandingOrderAmount -= y;

	    startedProdBatches++;
	    bcnt ++;
	}
	if (Demo.verbose) System.out.println( "At t=" + t+", " + getName() + " has put "+bcnt + " batches to needProd");
	needProd.step(state); // causes offerReceivers() to the prodDelay
	return (bcnt>0);
    }
    
    public String report() {

	String ba = (prototype instanceof Batch) ? " ba" : "";
	
	String s =// "[" + cname()
	    //+ "."+getName()+ "("+getTypical().getName()+")" +
	    "Ever ordered="+everOrdered+
	    "; ever started production="+	startedProdBatches+ " ba" +
	    ". Of this, "+
	    " still in factory=" + Util.ifmt(needProd.getAvailable()) + "+" + Util.ifmt(prodDelay.getDelayed()) + ba +
	    ", on truck " +  Util.ifmt(needTrans.getAvailable()) + "+" + Util.ifmt(transDelay.getDelayed()) + ba + 
	    ", in QA " +  Util.ifmt(needQa.getAvailable()) +  "+" +  Util.ifmt(qaDelay.getDelayed());
	if (transDelay.getAvailable()>0) s += "+" +  (long)transDelay.getAvailable();
	s += ba + ". ";
	s += "QA discarded=" + qaDelay.badResource + " ("+qaDelay.badBatches+ ba + " ba)" +
	    ", QA released=" + qaDelay.releasedGoodResource + " ("+qaDelay.releasedBatches+" ba)";

	long missing = startedProdBatches -
	    (long)(needProd.getAvailable() +
		   prodDelay.getDelayed() +
		   needTrans.getAvailable() +
		   transDelay.getDelayed() +
		   needQa.getAvailable() +
		   qaDelay.getDelayed() +
		   qaDelay.badBatches+ qaDelay.releasedBatches);
	if ( prototype instanceof Batch &&     missing!=0) s += ". Missing " + missing + " ba";

	
	return wrap(s);
    } 


      String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
  
    /** For the "Named" interface. Maybe it should set the counters to 0... */
    public void reset(SimState state) {}
 
   /** Does nothing. */
    public void step(SimState state){}

}
