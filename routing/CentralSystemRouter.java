package routing;

import java.util.List;

import core.DTNHost;
import core.Settings;


/** Central system router designed for BayesTrustRouter */
public class CentralSystemRouter extends ActiveRouter {
	
	/* Store global trust for all nodes */
	private double[] GT;
	
	/* Whether the global state is initialized */
	private boolean flag = false;
	
	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public CentralSystemRouter(Settings s) {
		super(s);
	}
	
	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected CentralSystemRouter(CentralSystemRouter r) {
		super(r);
	}
	
	protected void init(List<DTNHost> hosts) {
		this.GT = new double[hosts.size()];
		for(int i=0;i<hosts.size();i++)
			this.GT[i] = 1.0/hosts.size();
		
		this.flag = true;
	}
	
	
	public void update(List<DTNHost> hosts) {
		if(!this.flag)
			init(hosts);
		
		/* Calculate the scaling parameter of welfare model, 
		 * corresponding to "a" in the paper*/
		double[] a = new double[hosts.size()];
		for(int i=0;i<hosts.size();i++) {
			double [] weights = hosts.get(i).getRouter().getWeights();
			double totalWeights = 0.0;
			for(int j=0 ;j < hosts.size();j++) 
				totalWeights += weights[j];
			
			//totalWeights = 0.0 or 1.0 
			if(totalWeights > 0.1)
				a[i] = 0.15;
			else
				a[i] = 1;
		}
		
		/* Calculate the state transition matrix, corresponding to "T" in the paper*/
		double [][] T = new double[hosts.size()][hosts.size()];
		
		for(DTNHost host: hosts) {
			for(int i=0;i<hosts.size();i++) {
				T[host.getAddress()][i] = 0.85*host.getRouter().getWeights()[i] + 
						a[host.getAddress()]*this.GT[i];
			}
		}
		
		/* Update global trust */
		double[] tmp = new double[hosts.size()];
		for(int i=0;i<hosts.size();i++) {
			for(int j=0;j<hosts.size();j++) {
				tmp[i] += this.GT[j]*T[j][i];
			}
		}
		this.GT = tmp;
		
	}
	
	
	
	@Override
	public MessageRouter replicate() {
		// TODO Auto-generated method stub
		return null;
	}

}
