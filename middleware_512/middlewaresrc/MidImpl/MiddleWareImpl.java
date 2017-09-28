package MidImpl;
import ResInterface.*;
import MidInterface.*;

import java.util.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RMISecurityManager;

public class MiddleWareImpl implements MiddleWare 
{
	static ResourceManager rm_flight = null;
    static ResourceManager rm_car = null;
    static ResourceManager rm_room = null;

    protected RMHashtable m_itemHT = new RMHashtable();

    public static void main(String args[]) {
        // Figure out where server is running
        String server_flight = "localhost";
        String server_car = "localhost";
        String server_room = "localhost";

        int port_local = 1088;
        int port = 1099;


        if (args.length == 3) {
            server_flight = args[0];
            server_car = args[1];
            server_room = args[2];
            //port = Integer.parseInt(args[1]);
        } else if (args.length != 3) {
            System.err.println ("Wrong usage");
            System.out.println("Usage: java MidImpl.MiddleWareImpl [server_flight] [server_car] [server_room]");
            System.exit(1);
        }

        try {
            // create a new Server object
            MiddleWareImpl obj = new MiddleWareImpl();
            // dynamically generate the stub (client proxy)
            MiddleWare mw = (MiddleWare) UnicastRemoteObject.exportObject(obj, 0);            
            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry(port_local);
            registry.rebind("TripMiddleWare", mw);

            // get a reference to the rmiregistry
            Registry registry_flight = LocateRegistry.getRegistry(server_flight, port);
            Registry registry_car = LocateRegistry.getRegistry(server_car, port);
            Registry registry_room = LocateRegistry.getRegistry(server_room, port);

            // get the proxy and the remote reference by rmiregistry lookup
            rm_flight = (ResourceManager) registry_flight.lookup("FlightRM");
            rm_car = (ResourceManager) registry_car.lookup("CarRM");
            rm_room = (ResourceManager) registry_room.lookup("RoomRM");

            
            if(rm_flight!=null)
            {
                System.out.println("Successful");
                System.out.println("Connected to Flight RM");
            }
            else
            {
                System.out.println("Unsuccessful connection to Flight RM");
            }

            if(rm_car!=null)
            {
                System.out.println("Successful");
                System.out.println("Connected to Car RM");
            }
            else
            {
                System.out.println("Unsuccessful connection to Car RM");
            }

            if(rm_room!=null)
            {
                System.out.println("Successful");
                System.out.println("Connected to Room RM");
            }
            else
            {
                System.out.println("Unsuccessful connection to Room RM");
            }
            // make call on remote method


            System.err.println("MiddleWare Server ready");
        } catch (Exception e) {
            System.err.println("MiddleWare Server exception: " + e.toString());
            e.printStackTrace();
        }

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
    }
     
    public MiddleWareImpl() throws RemoteException {
    }

    // Reads a data item
    private RMItem readData( int id, String key )
    {
        synchronized(m_itemHT) {
            return (RMItem) m_itemHT.get(key);
        }
    }

    // Writes a data item
    private void writeData( int id, String key, RMItem value )
    {
        synchronized(m_itemHT) {
            m_itemHT.put(key, value);
        }
    }
    
    // Remove the item out of storage
    protected RMItem removeData(int id, String key) {
        synchronized(m_itemHT) {
            return (RMItem)m_itemHT.remove(key);
        }
    }
    
    // Create a new flight, or add seats to existing flight
    //  NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
        throws RemoteException
    {
        return rm_flight.addFlight(id,flightNum,flightSeats,flightPrice);
    }


    
    public boolean deleteFlight(int id, int flightNum)
        throws RemoteException
    {
        return rm_flight.deleteFlight(id, flightNum);
    }



    // Create a new room location or add rooms to an existing location
    //  NOTE: if price <= 0 and the room location already exists, it maintains its current price
    public boolean addRooms(int id, String location, int count, int price)
        throws RemoteException
    {
    	return rm_room.addRooms(id, location, count, price);
    }

    // Delete rooms from a location
    public boolean deleteRooms(int id, String location)
        throws RemoteException
    {
        return rm_room.deleteRooms(id, location);
    }

    // Create a new car location or add cars to an existing location
    //  NOTE: if price <= 0 and the location already exists, it maintains its current price
    public boolean addCars(int id, String location, int count, int price)
        throws RemoteException
    {
        return rm_car.addCars(id, location, count, price);
    }


    // Delete cars from a location
    public boolean deleteCars(int id, String location)
        throws RemoteException
    {
    	return rm_car.deleteCars(id, location);
    }



    // Returns the number of empty seats on this flight
    public int queryFlight(int id, int flightNum)
        throws RemoteException
    {
    	return rm_flight.queryFlight(id, flightNum);
    }

    // Returns the number of reservations for this flight. 
//    public int queryFlightReservations(int id, int flightNum)
//        throws RemoteException
//    {
//        Trace.info("RM::queryFlightReservations(" + id + ", #" + flightNum + ") called" );
//        RMInteger numReservations = (RMInteger) readData( id, Flight.getNumReservationsKey(flightNum) );
//        if ( numReservations == null ) {
//            numReservations = new RMInteger(0);
//        } // if
//        Trace.info("RM::queryFlightReservations(" + id + ", #" + flightNum + ") returns " + numReservations );
//        return numReservations.getValue();
//    }


    // Returns price of this flight
    public int queryFlightPrice(int id, int flightNum )
        throws RemoteException
    {
    	return rm_flight.queryFlightPrice(id, flightNum);
    }


    // Returns the number of rooms available at a location
    public int queryRooms(int id, String location)
        throws RemoteException
    {
    	return rm_room.queryRooms(id, location);
    }


    
    // Returns room price at this location
    public int queryRoomsPrice(int id, String location)
        throws RemoteException
    {
    	return rm_room.queryRoomsPrice(id, location);
    }


    // Returns the number of cars available at a location
    public int queryCars(int id, String location)
        throws RemoteException
    {
    	return rm_car.queryCars(id, location);
    }


    // Returns price of cars at this location
    public int queryCarsPrice(int id, String location)
        throws RemoteException
    {
    	return rm_car.queryCarsPrice(id, location);
    }

    // Returns data structure containing customer reservation info. Returns null if the
    //  customer doesn't exist. Returns empty RMHashtable if customer exists but has no
    //  reservations.
    // public RMHashtable getCustomerReservations(int id, int customerID)
    //     throws RemoteException
    // {
    //    return rm.getCustomerReservations(id, customerID);
    // }

    // return a bill
    public String queryCustomerInfo(int id, int customerID)
        throws RemoteException
    {
        Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + ") called" );
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        if ( cust == null ) {
            Trace.warn("RM::queryCustomerInfo(" + id + ", " + customerID + ") failed--customer doesn't exist" );
            return "";   // NOTE: don't change this--WC counts on this value indicating a customer does not exist...
        } else {
                String s = cust.printBill();
                Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + "), bill follows..." );
                System.out.println( s );
                return s;
        } // if
    }

    // customer functions
    // new customer just returns a unique customer identifier
    
    public int newCustomer(int id)
        throws RemoteException
    {
        Trace.info("INFO: RM::newCustomer(" + id + ") called" );
        // Generate a globally unique ID for the new customer
        int cid = Integer.parseInt( String.valueOf(id) +
                                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                                String.valueOf( Math.round( Math.random() * 100 + 1 )));
        Customer cust = new Customer( cid );
        writeData( id, cust.getKey(), cust );
        Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid );
        return cid;
    }

    // I opted to pass in customerID instead. This makes testing easier
    public boolean newCustomer(int id, int customerID )
        throws RemoteException
    {
    	Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") called" );
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        if ( cust == null ) {
            cust = new Customer(customerID);
            writeData( id, cust.getKey(), cust );
            Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") created a new customer" );
            return true;
        } else {
            Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") failed--customer already exists");
            return false;
        } // else
    }


    // Deletes customer from the database. 
    public boolean deleteCustomer(int id, int customerID)
        throws RemoteException
    {
    	Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") called" );
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        if ( cust == null ) {
            Trace.warn("RM::deleteCustomer(" + id + ", " + customerID + ") failed--customer doesn't exist" );
            return false;
        } else {            
            // Increase the reserved numbers of all reservable items which the customer reserved. 
            RMHashtable reservationHT = cust.getReservations();
            for (Enumeration e = reservationHT.keys(); e.hasMoreElements();) {        
                String reservedkey = (String) (e.nextElement());
                ReservedItem reserveditem = cust.getReservedItem(reservedkey);
                Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") has reserved " + reserveditem.getKey() + " " +  reserveditem.getCount() +  " times"  );
                ReservableItem item  = (ReservableItem) readData(id, reserveditem.getKey());
                Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") has reserved " + reserveditem.getKey() + "which is reserved" +  item.getReserved() +  " times and is still available " + item.getCount() + " times"  );
                item.setReserved(item.getReserved()-reserveditem.getCount());
                item.setCount(item.getCount()+reserveditem.getCount());
            }
            
            // remove the customer from the storage
            removeData(id, cust.getKey());
            
            Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") succeeded" );
            return true;
        } // if
    }



    /*
    // Frees flight reservation record. Flight reservation records help us make sure we
    // don't delete a flight if one or more customers are holding reservations
    public boolean freeFlightReservation(int id, int flightNum)
        throws RemoteException
    {
        Trace.info("RM::freeFlightReservations(" + id + ", " + flightNum + ") called" );
        RMInteger numReservations = (RMInteger) readData( id, Flight.getNumReservationsKey(flightNum) );
        if ( numReservations != null ) {
            numReservations = new RMInteger( Math.max( 0, numReservations.getValue()-1) );
        } // if
        writeData(id, Flight.getNumReservationsKey(flightNum), numReservations );
        Trace.info("RM::freeFlightReservations(" + id + ", " + flightNum + ") succeeded, this flight now has "
                + numReservations + " reservations" );
        return true;
    }
    */

    
    // Adds car reservation to this customer. 
    public boolean reserveCar(int id, int customerID, String location)
        throws RemoteException
    {
    	Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        String key = ("car-" + location).toLowerCase();

        if ( cust == null ) {
            Trace.warn("RM::reserveCar( " + id + ", " + customerID + ", " + key + ", "+location+")  failed--customer doesn't exist" );
            return false;
        } else {
            rm_car.reserveCar(id, customerID, location);
            cust.reserve( key, location, rm_car.queryCarsPrice(id, location));      
            writeData( id, cust.getKey(), cust );
            return true;
        }
    }

    // Adds room reservation to this customer. 
    public boolean reserveRoom(int id, int customerID, String location)
        throws RemoteException
    {
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        String key = ("room-" + location).toLowerCase();

        if ( cust == null ) {
            Trace.warn("RM::reserveRoom( " + id + ", " + customerID + ", " + key + ", "+location+")  failed--customer doesn't exist" );
            return false;
        } else {
    	    rm_room.reserveRoom(id, customerID, location);
            cust.reserve( key, location, rm_room.queryRoomsPrice(id, location));      
            writeData( id, cust.getKey(), cust );
            return true;
        }
    }
    // Adds flight reservation to this customer.  
    public boolean reserveFlight(int id, int customerID, int flightNum)
        throws RemoteException
    {
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        String key = ("flight-" + flightNum).toLowerCase();

        if ( cust == null ) {
            Trace.warn("RM::reserveFlight( " + id + ", " + customerID + ", " + key + ", "+String.valueOf(flightNum)+")  failed--customer doesn't exist" );
            return false;
        } else {
            rm_flight.reserveFlight(id, customerID, flightNum);
            cust.reserve( key, String.valueOf(flightNum), rm_flight.queryFlightPrice(id, flightNum));      
            writeData( id, cust.getKey(), cust );
            return true;
        }
    }

    public RMHashtable getCustomerReservations(int id, int customerID)
        throws RemoteException
    {
        Trace.info("RM::getCustomerReservations(" + id + ", " + customerID + ") called" );
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        if ( cust == null ) {
            Trace.warn("RM::getCustomerReservations failed(" + id + ", " + customerID + ") failed--customer doesn't exist" );
            return null;
        } else {
            return cust.getReservations();
        } // if
    }
    
    // Reserve an itinerary 
    public boolean itinerary(int id,int customer,Vector flightNumbers,String location,boolean Car,boolean Room)
        throws RemoteException
    {
        boolean correct = false;
        if (Car) {
            correct = reserveCar(id, customer, location);
            if (!correct) {
                return false;
            }
        }
        if (Room) {
            correct = reserveRoom(id, customer, location);
            if (!correct) {
                return false;
            }
        }
        if (flightNumbers.size()==0) {
            return false;
        }
        for (int i = 0; i < flightNumbers.size() ;i++ ) {
            correct = reserveFlight(id, customer, Integer.parseInt((String)flightNumbers.elementAt(i)));
            if (!correct) {
                return false;
            }
        }
        return true;
    }

    public boolean reserveItinerary(int id,int customer,Vector flightNumbers,String location, boolean Car, boolean Room)
        throws RemoteException
    {
        return false;
    }
}