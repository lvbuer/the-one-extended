package interfaces;

import java.util.Collection;
import java.util.HashMap;

import core.CBRConnection;
import core.Connection;
import core.NetworkInterface;
import core.Settings;

/**
 * A simple Network Interface that provides a constant bit-rate and unicast service.
 */
public class SimpleUnicastInterface extends NetworkInterface {
	
	/**
	 * Reads the interface settings from the Settings file
	 */
	public SimpleUnicastInterface(Settings s)	{
		super(s);
	}
		
	/**
	 * Copy constructor
	 * @param ni the copied network interface object
	 */
	public SimpleUnicastInterface(SimpleUnicastInterface ni) {
		super(ni);
	}
	

	@Override
	public NetworkInterface replicate() {
		// TODO Auto-generated method stub
		return new SimpleUnicastInterface(this);
	}

	
	/**
	 * Tries to connect this host to another host. The other host must be
	 * active and within range of this host for the connection to succeed. 
	 * @param anotherInterface The interface to connect to
	 */
	public void connect(NetworkInterface anotherInterface) {
		if (isScanning()  
				&& anotherInterface.getHost().isRadioActive() 
				&& isWithinRange(anotherInterface) 
				&& !isConnected(anotherInterface)
				&& (this != anotherInterface)
				&& (!this.getHost().toString().equals(anotherInterface.getHost().toString()))) {
			// new contact within range
			// connection speed is the lower one of the two speeds 
			int conSpeed = anotherInterface.getTransmitSpeed(this);
			if (conSpeed > this.transmitSpeed) {
				conSpeed = this.transmitSpeed; 
			}

			Connection con = new CBRConnection(this.host, this, 
					anotherInterface.getHost(), anotherInterface, conSpeed);
			
			//super.getConnections().add(con);
			this.connections.add(con);
			notifyConnectionListeners(CON_UP, anotherInterface.getHost());
			// inform routers about the connection
			this.host.connectionUp(con);
			
		}
	}
	
	/**
	 * Updates the state of current connections (i.e. tears down connections
	 * that are out of range and creates new ones).
	 */
	public void update() {
		if (optimizer == null) {
			return; /* nothing to do */
		}
		
		// First break the old ones
		optimizer.updateLocation(this);
		
		//The same transmit interface, but different transmit ranges, update the ConnectivityGrid
		for(ConnectivityGrid grid: ConnectivityGrid.gridobjects.values()) {
			if(optimizer != grid && optimizer.getInterfaceName() != null && grid.getInterfaceName() != null 
					&& optimizer.getInterfaceName().equals(grid.getInterfaceName())) {
				grid.updateLocation(this);
			}
		}
		
		for (int i=0; i<this.connections.size(); ) {
			Connection con = this.connections.get(i);
			NetworkInterface anotherInterface = con.getOtherInterface(this);

			// all connections should be up at this stage
			assert con.isUp() : "Connection " + con + " was down!";

			if (!isWithinRange(anotherInterface)) {
				disconnect(con,anotherInterface);
				connections.remove(i);
			}
			else {
				i++;
			}
		}
		// Then find new possible connections
		Collection<NetworkInterface> interfaces =
			optimizer.getNearInterfaces(this);
		for (NetworkInterface i : interfaces) {
			connect(i);
		}
		

	}

	/** 
	 * Creates a connection to another host. This method does not do any checks
	 * on whether the other node is in range or active 
	 * @param anotherInterface The interface to create the connection to
	 */
	public void createConnection(NetworkInterface anotherInterface) {
		if (!isConnected(anotherInterface) && (this != anotherInterface)) {    			
			// connection speed is the lower one of the two speeds 
			int conSpeed = anotherInterface.getTransmitSpeed(this);
			if (conSpeed > this.transmitSpeed) {
				conSpeed = this.transmitSpeed; 
			}

			Connection con = new CBRConnection(this.host, this, 
					anotherInterface.getHost(), anotherInterface, conSpeed);
			connect(con,anotherInterface);
		}
	}
	
	/**
	 * Returns true if another interface is within radio range of this interface.
	 * @param anotherInterface The another interface
	 * @return True if the interface is within range, false if not
	 */
	protected boolean isWithinRange(NetworkInterface anotherInterface) {
		return this.host.getLocation().distance(
				anotherInterface.getHost().getLocation()) <= getTransmitRange();
	}
	
	/**
	 * Returns a string representation of the object.
	 * @return a string representation of the object.
	 */
	public String toString() {
		return "SimpleUnicastInterface " + super.toString();
	}

}
