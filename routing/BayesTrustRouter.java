package routing;

import java.util.HashMap;
import java.util.Random;

import core.Connection;
import core.DTNHost;
import core.Settings;
import core.SimClock;

/** 
 * Router for trust management based on Bayesian theory, implemented from the article 
 * "BayesTrust and VehicleRank: Constructing an Implicit Web of Trust in VANET"
 * */
public class BayesTrustRouter extends ActiveRouter {
	/* Router's setting namespace ({@value})*/
	public static final String BAYESTRUST_NS = "BayesTrustRouter";
	/* Speed threshold to classify nodes as normal or malicious */
	private double spThres;
	
	public static Random rng = new Random(SimClock.getIntTime());
	
	/* LTVs store computed local trust and local trust parameters.
	 * LTVs ---> {DTNHost:[n, y, a, b, LTV],}, where n represents the number of encounters, 
	 * y represents the number of normal behavior based on speed times,
	 * a and b are the parameters of the beta distribution */
	public HashMap<DTNHost, Double[]> LTVs;
	/* The weight of a node's local trust in all local trusts */
	public double[] weights;
	

	public BayesTrustRouter(Settings s) {
		super(s);
		// TODO Auto-generated constructor stub
		
		init();
	}

	public BayesTrustRouter(ActiveRouter r) {
		super(r);
		// TODO Auto-generated constructor stub
		
		init();
	}
	
	public void init() {
		Settings bayes = new Settings(BAYESTRUST_NS);
		
		int num = bayes.getInt("nrofHosts");
		this.spThres = bayes.getDouble("speedThreshold");
		this.LTVs = new HashMap<DTNHost, Double[]>();
		this.weights = new double[num];
		
	}
	
	/**
	 * Update the number of encounters and other parameters with other node
	 * @param con The connection object whose state changed
	 */
	@Override
	public void changedConnection(Connection con) {
		if (con.isUp()) {
			DTNHost otherHost = con.getOtherNode(getHost());
			//Determine if behavior is normal
			if(otherHost.getPath()!=null && otherHost.getPath().getSpeed() <= this.spThres) {
				if(this.LTVs.containsKey(otherHost)) {
					Double[] tmp = this.LTVs.get(otherHost);
					tmp[0] += 1;
					tmp[1] += 1;
					updateLTV(tmp);
				}
				else {
					Double[] tmp = new Double[] {1.0 ,1.0, 1.0, 1.0, 0.0};
					this.LTVs.put(otherHost, tmp);
				}
			}
			else {
				if(this.LTVs.containsKey(otherHost)) {
					Double[] tmp = this.LTVs.get(otherHost);
					tmp[0] += 1;
					updateLTV(tmp);
				}
				else {
					Double[] tmp = new Double[] {1.0 ,0.0, 1.0, 1.0, 0.0};
					this.LTVs.put(otherHost, tmp);
				}
			}
		}
	}
	
	@Override
	public void update() {
		super.update();
		
		if (isTransferring() || !canStartTransfer()) {
			return; // transferring, don't try other connections yet
		}
		
		// Try first the messages that can be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return; // started a transfer, don't try others (yet)
		}
		
		// then try any/all message to any/all connection
		this.tryAllMessagesToAllConnections();
	}
	

	/**
	 * Update local trust
	 * @param host Bayesian trust parameter
	 */
	public void updateLTV(Double[] host) {		
		if(host[0] <= host[2] + host[3] + 1)
			return ;
		
		host[4] = (host[1] + host[2]) / (host[0] + host[2] + host[3]);
		host[2] = host[1] + host[2];
		host[3] = host[0] - host[1] + host[3];
		
		updateWeights();
		
	}
	
	/** Update the weight of the local trust among all trusts */
	public void updateWeights() {	
		double totalWeights = 0.0;
		for(Double[] tmp: this.LTVs.values()) {
			totalWeights += tmp[4];
		}
		
		for(int i=0;i<this.weights.length;i++)
			this.weights[i] = 0.0;
		
		for(DTNHost host: this.LTVs.keySet()) {
			this.weights[host.getAddress()] =  this.LTVs.get(host)[4]/totalWeights;
		}
		
	}

	public double[] getWeights(){
		return this.weights;
	}
	
	
	@Override
	public MessageRouter replicate() {
		// TODO Auto-generated method stub
		return new BayesTrustRouter(this);
	}

}
