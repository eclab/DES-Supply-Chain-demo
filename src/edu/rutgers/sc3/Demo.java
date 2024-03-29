package  edu.rutgers.sc3;

import edu.rutgers.supply.*;

import java.io.*;
import java.util.*;
import java.text.*;

import sim.engine.*;
import sim.util.*;
import sim.util.distribution.*;
import sim.des.*;

import ec.util.MersenneTwisterFast;

import edu.rutgers.util.*;
import edu.rutgers.supply.Disruptions.Disruption;

import sim.portrayal.grid.*;

import sim.portrayal.network.*;
import sim.portrayal.continuous.*;
import sim.display.*;
import sim.portrayal.simple.*;
import sim.portrayal.*;
import javax.swing.*;
import java.awt.Color;
import java.awt.*;
import sim.field.network.*;
import sim.des.portrayal.*;


/** The main class for the SC-3 model

<pre>
Demo demo = ....;
run simulation
</pre>

 */
public class Demo extends SimState {

    public String version = "1.010";

    
    /** Set this to true to print a lot of stuff */
    static boolean verbose=false;

    /** Set this to true to print less stuff, and turn off all interactive things */
    static boolean quiet=false;
    static public void setQuiet(boolean _quiet) { quiet = _quiet; }

    /** 1 means just have Small; 2 means have both Small and Large */
    static int M = 2;
    static public void setM(int _M) { M = _M; }
    

    
    public DES2D field = new DES2D(200, 200);

    /** Should be set by MakeDemo.newInstance(), and then used in start() */
    protected Config config=null;
    protected Disruptions disruptions = null;
    /** Sets the disruption scenario for this model */
    public void setDisruptions(Disruptions _disruptions) {
	disruptions = _disruptions;
    }

    
    Vector<Disruption> hasDisruptionToday(Disruptions.Type type, String unit) {
	if (disruptions == null) return new Vector<Disruption>();
	double time = schedule.getTime();
	return disruptions.hasToday(type, unit, time);
    }


    /** Used to look up supply chain elements by name */
    private HashMap<String,Steppable> addedNodes = new HashMap<>();
    Steppable lookupNode(String name) { return addedNodes.get(name); }

    Vector<Reporting> reporters = new Vector<>();

    
    private int ordering = 0;
    void add(Steppable z) {
	if (z instanceof Named) {
	    String name = ((Named)z).getName();
	    if (addedNodes.put(name, z)!=null) throw new IllegalArgumentException("Attempt to add duplicate node named " + name);
	}
	if (z instanceof Reporting) reporters.add((Reporting)z);
	IterativeRepeat ir =	schedule.scheduleRepeating(z, ordering++, 1.0);
    }

    void addFiller(String text) {
	reporters.add(new Filler(text));
    }
    
    public Demo(long seed)    {
	super(seed);
	Disruptions.setSc2BackwardCompatible(false);

	if (verbose) System.out.println("sc3.Demo()");
    }

  /** Here, the supply network elements are added to the Demo object */
    public void start(){
	super.start();
	if (!quiet) System.out.println("Demo.start");
	if (!quiet) System.out.println("Disruptions=" + disruptions);
	initSupplyChain();
	final int CENSUS_INTERVAL=360;
	if (verbose) schedule.scheduleRepeating(new Reporter(), CENSUS_INTERVAL);
	if (!quiet) System.out.println("SC3 DES/MASON simulation, ver=" + version +", config=" + config.readFrom);
	if (verbose) doReport("Start");
    }

    //-- Not needed: implemented as SS source
    // Production 	fiberSupplier, resinSupplier;
    Production aluminumSupplier, cellPMSupplier,
	cellRMSupplier, coverglassSupplier;
    Production 	adhesiveSupplier, diodeSupplier;


    Production prepregProd, substrateProd[] = new Production[2], cellProd, cellAssembly, cellPackaging;
    Production[] arrayAssembly =  new Production[2];
    
 
    Batch fiberBatch, resinBatch, prepregBatch, aluminumBatch;
    Batch[] substrate = new Batch[2], array = new Batch[2];
    Batch cellBatch, packagedCellBatch, cellRMBatch, coverglassBatch, coverglassAssemblyBatch, diodeBatch, adhesiveBatch;

    EndCustomer[] endCustomer = new EndCustomer[2];

    private Batch batch(String name) throws IllegalInputException {
	CountableResource r = new CountableResource(name, 1);
	Batch b = Batch.mkPrototype(r, config);
	return b;
    }

    private Batch batchU(String name) throws IllegalInputException {
	UncountableResource r = new UncountableResource(name, 1);
	Batch b = Batch.mkPrototype(r, config);
	return b;
    }

    /** The main part of the start() method. It is taken into a separate
	method so that it can also be used from auxiliary tools, such as 
	GraphAnalysis.
    */
    void initSupplyChain() {
	try {
	    //Patient.init(config);
	    
	    //addFiller("   --- EE BRANCH ---");		      

	    Batch fiberBatch = batchU("fiber"),
		resinBatch = batchU("resin"),
		prepregBatch = batchU("prepreg"),
		aluminumBatch = batchU("aluminum"),
		adhesiveBatch = batchU("adhesive");

	    Batch substrateBatch[] = {
		batch("substrateSmall"),
		batch("substrateLarge")};


	    Batch arrayBatch[] = {
		batch("arraySmall"),
		batch("arrayLarge")};


	    Batch
		diodeBatch = batch("diode"),
		cellBatch = batch("cell"),
		cellRMBatch = batch("cellRM"),
		cellPMBatch = batch("cellPM"),
		coverglassBatch = batch("coverglass");


	    addFiller("   --- SUBSTRATES ---");		      	    
	    prepregProd = new Production(this, "prepregProd", config,
					 new Resource[] {fiberBatch, resinBatch},
					 prepregBatch);
	    add(prepregProd);


	    for(int j=M-1; j>=0; j--) { 
	    
		substrateProd[j] = new Production(this,
						  substrateBatch[j].getUnderlyingName() + "Prod",
						  config,
						  new Resource[] {prepregBatch, aluminumBatch},
						  substrateBatch[j]);
		add(substrateProd[j]);
	    }

	    addFiller("   --- CELL ---");
	    //-- shared input buffers; see config file for details
	    cellProd =  new Production(this, "cellProd", config,
				       new Resource[] {cellRMBatch, cellBatch, coverglassBatch},
				       cellBatch);
	    add(cellProd);

	    cellAssembly =  new Production(this, "cellAssembly", config,
					   new Resource[] {cellRMBatch, cellBatch, coverglassBatch},
					   cellBatch);
	    add(cellAssembly);

	    cellPackaging =  new Production(this, "cellPackaging", config,
					   new Resource[] {cellBatch, cellPMBatch},
					   cellBatch);
	    add(cellPackaging);

	    
	    //prepregProd.setQaReceiver(substrateProd[0].getEntrance(0), 1.0);
	   
 	    addFiller("   --- ARRAY ASSEMBLY ---");

	    for(int j=M-1; j>=0; j--) {
		Batch out = arrayBatch[j];
		String name = out.getUnderlyingName() + "Assembly";
		Resource[] inputs = {substrateBatch[0],substrateBatch[1], cellBatch, adhesiveBatch, diodeBatch};
		arrayAssembly[j] = new Production(this,
						  name,
						  config,
						  inputs,
						  out);
		add(arrayAssembly[j]);
	    }

	    //-- End customers
	    addFiller("   --- END CUSTOMER ---");

	    for(int j=M-1; j>=0; j--) {
		endCustomer[j] = new EndCustomer(this, 
						 arrayBatch[j].getUnderlyingName() + "Customer",
						 config,
						 arrayBatch[j]);
		add(endCustomer[j]);
	    }

	    
	    //-- Link up the production nodes
	    for(Steppable q: addedNodes.values()) {
		if (q instanceof Production) {
	    	    ((Production)q).linkUp(addedNodes);
		} else if (q instanceof EndCustomer) {
		    ((EndCustomer)q).linkUp(addedNodes);
		}
	    }

	    /*
	    eeRMSupplier = new Production(this, "eeRMSupplier", config,
					  new Resource[] {},
					  rmEEBatch);
	    add(eeRMSupplier);
				       
	    eeCmoProd = new Production(this, "eeCmoProd", config,
				       new Resource[] {rmEEBatch},
				       eeBatch);
	    
	    eeRMSupplier.setQaReceiver(eeCmoProd.getEntrance(0), 1.0);	
	    add(eeCmoProd);

	    eeCmoBackupProd = new Production(this, "eeCmoBackupProd", config,
				       new Resource[] {rmEEBatch},
				       eeBatch);
	    
	    eeCmoBackupProd.shareInputStore(eeCmoProd);
	    eeCmoBackupProd.sharePlan(eeCmoProd);
	    add(eeCmoBackupProd);

	    
	    
	    CountableResource pmEE = new CountableResource("PMEE", 1);
	    Batch pmEEBatch = Batch.mkPrototype(pmEE, config);

	    //----
	    eePMSupplier = new Production(this, "eePMSupplier", config,
					  new Resource[] {},
					  pmEEBatch);
	    add(eePMSupplier);
				       
	    

	    //---
	    eePackaging = new Production(this, "eePackaging", config,
					 new Resource[] {eeBatch, pmEEBatch},
					 eeBatch);
	    eePackaging.setNoPlan(); // driven by inputs
	    add(eePackaging);

	    eeCmoProd.setQaReceiver(eePackaging.getEntrance(0), 1.0);
	    eeCmoBackupProd.setQaReceiver(eePackaging.getEntrance(0), 1.0);	
	    eePMSupplier.setQaReceiver(eePackaging.getEntrance(1), 1.0);	

	    eeMedTech = new MedTech(this, "eeMedTech",
				    new Production[] {
					eeCmoProd,eeRMSupplier,eePMSupplier},
				    null);
	    
	    add(eeMedTech);

	    eeDC = new Pool(this, "eeDC", config,  eeBatch, new String[0]);
	    add(eeDC);

	    Delay d = eePackaging.mkOutputDelay(eeDC);
	    eePackaging.setQaReceiver(d, 1.0);	

	    eeDP = new Pool(this, "eeDP", config,  eeBatch, new String[0]);
	    add(eeDP);

	    eeHEP = new Pool(this, "eeHEP", config,  eeBatch, new String[0]);
	    add(eeHEP);

	    //---- DS production chain ----	    

	    addFiller("   --- DS BRANCH ---");		      


	    
	    dsRMSupplier = new Production(this, "dsRMSupplier", config,
					  new Resource[] {},
					  rmDSBatch);
	    add(dsRMSupplier);

	    BatchDivider dsRMBD = new BatchDivider(this, rmDSBatch,
						   dsRMSupplier.getPara().getDouble("outBatch"));

	    dsRMSupplier.setQaReceiver(dsRMBD, 1.0);

	    dsRMSplitter = new Splitter(this, rmDSBatch);
	    
	    

	    
	    dsRMBD.addReceiver(dsRMSplitter);
	    
	    dsCmoProd = new Production(this, "dsCmoProd", config,
				       new Resource[] {rmDSBatch},
				       dsBatch);
	    dsCmoProd.setNoPlan(); // driven by inputs (has no safety stock)

	    add(dsCmoProd);


	    dsCmoBackupProd = new Production(this, "dsCmoBackupProd", config,
					     new Resource[] {rmDSBatch},
					     dsBatch);
	    dsCmoBackupProd.shareInputStore(dsCmoProd);
	    dsCmoBackupProd.setNoPlan(); // driven by inputs (has no safety stock)
	    //dsCmoBackupProd.sharePlan(dsCmoProd);

	    add(dsCmoBackupProd);
	    
	    dsRMSplitter.addReceiver(dsCmoProd.mkInputDelay(0), 0.08);

	    dsProd = new Production(this, "dsProd", config,
				    new Resource[] {rmDSBatch},
				    dsBatch);

	    add(dsProd);
	    dsRMSplitter.addReceiver(dsProd.mkInputDelay(0), 0.90);
	    // DS CMO Prod has no QA stage of its own, and uses DS Prod's QA
	    dsCmoProd.setQaReceiver( dsProd.getQaEntrance(), 1.0);


	    CountableResource pmDS = new CountableResource("PMDS", 1);
	    Batch pmDSBatch = Batch.mkPrototype(pmDS, config);


	    //----
	    dsPMSupplier = new Production(this, "dsPMSupplier", config,
					  new Resource[] {},
					  pmDSBatch);
	    add(dsPMSupplier);
				       
	    dsPackaging = new Production(this, "dsPackaging", config,
					 new Resource[] {dsBatch, pmDSBatch},
					 dsBatch);
	    dsPackaging.setNoPlan(); // driven by inputs
	    add(dsPackaging);
	    
	    dsProd.setQaReceiver(dsPackaging.getEntrance(0), 1.0);	
	    dsPMSupplier.setQaReceiver(dsPackaging.getEntrance(1), 1.0);	

	    dsMedTech = new MedTech(this, "dsMedTech",
				    new Production[] {
					//dsCmoProd,
					dsProd,
					dsRMSupplier,dsPMSupplier},
				    new double[] {0.8, 1, 1}
				    );
	    
	    add(dsMedTech);
	    	   
	    dsDC = new Pool(this, "dsDC", config,  dsBatch, new String[0]);
	    add(dsDC);

	    
	    dsPackaging.setQaReceiver(dsPackaging.mkOutputDelay(dsDC), 1.0);	
	    
	    
	    dsDP = new Pool(this, "dsDP", config,  dsBatch, new String[0]);
	    add(dsDP);

	    dsHEP = new Pool(this, "dsHEP", config,  dsBatch, new String[0]);
	    add(dsHEP);

	    
	    //-- link the pools, based on the "from1" fields in its ParaSet
	    eeDC.setSuppliers(addedNodes);
	    eeDP.setSuppliers(addedNodes);
	    eeHEP.setSuppliers(addedNodes);

	    dsDC.setSuppliers(addedNodes);
	    dsDP.setSuppliers(addedNodes);
	    dsHEP.setSuppliers(addedNodes);

	    addFiller("   --- PATIENTS ---");		      
	    
	    wpq = new WaitingPatientQueue(this, config);
	    add(wpq);
	    
	    
	    spp = new ServicedPatientPool(this, config, wpq, eeHEP, dsHEP);
	    add(spp);
	    */
	    
	} catch( IllegalInputException ex) {
	    System.out.println("Unable to create a model due to a problem with the configuration parameters:\n" + ex);
	    ex.printStackTrace(System.err);
	    System.exit(1);
	} catch(Exception ex) {
	    System.out.println("Exception:\n" + ex);
	    ex.printStackTrace(System.err);
	    System.exit(1);
	}
 
    }

    public void	finish() {
	if (!quiet) doReport("Finish");
	if (verbose) System.out.println("Closing logs");
	Charter.closeAll();
    }
    
    static class Reporter implements Steppable {
	public void step(SimState state) {
	    ((Demo)state).doReport("Report at t=" + state.schedule.getTime());
	}  
    }

    void doReport(String msg) {
	System.out.println("===== "+schedule.getTime() + ": " +
			   msg+" =================\n"
			   + report());
	System.out.println("================================================");
	
    }
    
    String report() {
	Vector<String> v= new Vector<>();
	/*
	v.add(eeRMSupplier.report());
	v.add(eePMSupplier.report());
	v.add(eeCmoProd.report());
	v.add(eePackaging.report());
	v.add(eeDC.report());
	v.add(eeDP.report());
	v.add(eeHEP.report());
	v.add(eeMedTech.report());
	v.add(wpq.report());
	v.add(spp.report());
	*/
	for(Reporting r: reporters) v.add(r.report());

	//	v.add(dsRMSplitter.report());

	// Create a CSV file with order tracks for each EndCustomer
	plotWaitingStats();
	EndCustomer.Stats[] stats = getWaitingStats();
       	EndCustomer.Stats awf=stats[0], awu=stats[1], aw = stats[2];
	
	Vector<String> w = new Vector<>();
	
	if (awf.cnt>0) 	w.add( " for "+awf.cnt+" filled orders " + awf.avgT   + " days");
	if (awu.cnt>0) 	w.add( " for "+awu.cnt+" unfilled orders " + awu.avgT  + " days so far");     
	if (aw.cnt>0) 	w.add( " for all "+aw.cnt+" orders " + aw.avgT     + " days so far");
	
	String s = 
	    (w.size()>0) ? "All customers: Avg waiting time" + String.join(",", w) + ".":
	    "No orders!";
	v.add(s);		

	
	return String.join("\n", v);
    }

    /** Computes wating-time statistics for a complete run *
       @return {statsForFilled, statsForUnfilled, statsForAll }
     */
    public EndCustomer.Stats[] getWaitingStats() {
	EndCustomer.Stats[] results = new EndCustomer.Stats[3];
	for(int i=0; i<results.length; i++) {
	    results[i] = new  EndCustomer.Stats();
	}
	
	for(int j=M-1; j>=0; j--) {
	    EndCustomer ec = endCustomer[j];
	    results[0].add(ec.avgWaitingFilled());
	    results[1].add(ec.avgWaitingUnfilled());
	    results[2].add(ec.avgWaitingAll());
	}

	return results;
    }

    /** Causes each customer print out the stats for all its orders,
	into its CSV file (Charter)
    */
    void plotWaitingStats() {
	for(int j=M-1; j>=0; j--) {
	    EndCustomer ec = endCustomer[j];
	    //System.out.println("DEBUG: Printing waiting stats for " + ec.getName());
	    ec.plotAllOrders();
	}
    }

        
    public static class MakesDemo implements  MakesSimState {
   
	/** The Config object contains the parameters for
	    various supply chain elements, read from a
	    config file
	*/
	final private Config config0;
	/** The disruption scenario specified on the command line. Null if none have been specified */
	final public Disruptions disruptions0;
	/** The data from the command line argument array, after the removal of options
	    interpreted by the constructor (such as -config XXX) will be put here. */
	public final String[] argvStripped;

	/** For use in RepeatTest */
	public int repeat=1;
	public boolean repeatSet = false;
	
	/** Initializes the Config and Disruptions structures from their respective
	    config files. 
	    @param argv The actual command line array. The constructor will look for
	    the -config and -disrupt options in it.
	 */
	public MakesDemo(String[] argv) throws IOException, IllegalInputException    {

	    String confPath = "../config/sc3.csv";
	    String chartsPath = "charts";
	    String disruptPath = null;

	    Vector<String> va = new Vector<String>();
	    for(int j=0; j<argv.length; j++) {
		boolean keep=false;
		String a = argv[j];
		if (a.equals("-verbose")) {
		    verbose = true;
		} else 	if (a.equals("-quiet")) {
		    quiet = true;
		} else if (a.equals("-config") && j+1<argv.length) {
		    confPath= argv[++j];
		} else if (a.equals("-disrupt") && j+1<argv.length) {
		    disruptPath= argv[++j];
		} else if (a.equals("-charts") && j+1<argv.length) {
		    chartsPath= argv[++j];
		} else if (a.equals("-repeat") && j+1<argv.length) {
		    repeat = Integer.parseInt(argv[++j]);
		    repeatSet = true;				    
		} else if (a.equals("-M") && j+1<argv.length) {
		    M =  Integer.parseInt(argv[++j]);
		    if (M<1 || M>2) throw new IllegalInputException("Illegal M=" + M+"; M may only be 1 or 2");
		} else {
		    keep = true;
		}

		if (keep) va.add(a);
	    }
	
	    File f= new File(confPath);
	    config0  = Config.readConfig(f);

	    disruptions0 = (disruptPath == null) ? null:
		 Disruptions.readList(new File(disruptPath));

	    // The chart directory. (The value "null" means no charting)
	    File logDir = (chartsPath.equals("/dev/null") ||
			   chartsPath.equals("null")) ? null:
		new File(chartsPath);
	    Charter.setDir(logDir);

	    
	    argvStripped = va.toArray(new String[0]);
	    
	}
	public java.lang.Class	simulationClass() {
	    return Demo.class;
	}	    
	public java.lang.reflect.Constructor[]	getConstructors() {
	    return Demo.class.getConstructors();
	}

	protected void initDemo(Demo demo) {
	    //demo.disruptions = new Disruptions();
	    //demo.disruptions.add( Disruptions.Type.ShipmentLoss, "RawMaterialSupplier", 40, 30);
	    demo.config = config0;
	    demo.disruptions = disruptions0;
	}
	
	public SimState	newInstance(long seed, java.lang.String[] args) {
	    Demo demo = new Demo(seed);
	    initDemo(demo);
	    return demo;
	}
    }


    
    /** Extracts a few command-line options we understand, and leaves
	the rest of them to MASON.
    */
    public static void main(String[] argv) throws IOException, IllegalInputException {

	MakesDemo maker = new MakesDemo(argv);
	argv = maker.argvStripped;

	for(int j=0; j<maker.repeat; j++) {
	    System.out.println("Run No. " + (j+1));
	    //doLoop(Demo.class, argv);
	    doLoop(maker, argv);
	}
	
	System.exit(0);
    }
    
    
}
