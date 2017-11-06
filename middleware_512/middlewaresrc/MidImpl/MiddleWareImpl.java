package MidImpl;
import ResInterface.*;
import MidInterface.*;
import LockManager.*;

import java.util.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RMISecurityManager;

public class MiddleWareImpl implements MiddleWare 
{
    public static final int READ = 0;
    public static final int WRITE = 1;
    // protected LockManager mw_locks;
    // protected int txn_counter; // should be moved to TM
    // protected ArrayList<Integer> active_txn; // should be moved to TM
	protected static ResourceManager rm_flight = null;
    protected static ResourceManager rm_car = null;
    protected static ResourceManager rm_room = null;
    static TransactionManager txn_manager = new TransactionManager();

    protected RMHashtable m_itemHT = new RMHashtable();

    public static void main(String args[]) {
        // Figure out where server is running
        String server_flight = "localhost";
        String server_car = "localhost";
        String server_room = "localhost";

        int port_local = 1088;
        int port = 2199;

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
        String s = "flight-" + flightNum;
        String key = s.toLowerCase();
        if (!txn_manager.requestLock(id, key, WRITE))
        {
            Trace.warn("RM::Lock failed--Can not acquire lock");
            return false;
        }
        int old_price = rm_flight.queryFlightPrice(id, flightNum);
        if (rm_flight.addFlight(id,flightNum,flightSeats,flightPrice))
        {
            String command = "newflight";
            txn_manager.active_txn.get(id).addHistory(command, Integer.toString(flightNum), Integer.toString(flightSeats), Integer.toString(old_price));
            return true;
        }
        else return false;
    }


    
    public boolean deleteFlight(int id, int flightNum)
        throws RemoteException
    {
        String s = "flight-" + flightNum;
        String key = s.toLowerCase();
        if (!txn_manager.requestLock(id, key, WRITE))
        {
            Trace.warn("RM::Lock failed--Can not acquire lock");
            return false;
        }
        // return rm_flight.deleteFlight(id, flightNum);
        int old_price = rm_flight.queryFlightPrice(id, flightNum);
        int old_count = rm_flight.queryFlight(id, flightNum);
        if (rm_flight.deleteFlight(id, flightNum))
        {
            String command = "deleteflight";
            txn_manager.active_txn.get(id).addHistory(command, Integer.toString(flightNum), Integer.toString(old_count), Integer.toString(old_price));
            return true;
        }
        else return false;
    }



    // Create a new room location or add rooms to an existing location
    //  NOTE: if price <= 0 and the room location already exists, it maintains its current price
    public boolean addRooms(int id, String location, int count, int price)
        throws RemoteException
    {
        String s = "room-" + location;
        String key = s.toLowerCase();
        if (!txn_manager.requestLock(id, key, WRITE))
        {
            Trace.warn("RM::Lock failed--Can not acquire lock");
            return false;
        }
        int old_price = rm_room.queryRoomsPrice(id, location);
        if (rm_room.addRooms(id, location, count, price))
        {
            String command = "newroom";
            txn_manager.active_txn.get(id).addHistory(command, location, Integer.toString(count), Integer.toString(old_price));
            return true;
        }
        else return false;
    }

    // Delete rooms from a location
    public boolean deleteRooms(int id, String location)
        throws RemoteException
    {
        String s = "room-" + location;
        String key = s.toLowerCase();
        if (!txn_manager.requestLock(id, key, WRITE))
        {
            Trace.warn("RM::Lock failed--Can not acquire lock");
            return false;
        }
        // return rm_room.deleteRooms(id, location);
        int old_price = rm_room.queryRoomsPrice(id, location);
        int old_count = rm_room.queryRooms(id, location);
        if (rm_room.deleteRooms(id, location))
        {
            String command = "deleteroom";
            txn_manager.active_txn.get(id).addHistory(command, location, Integer.toString(old_count), Integer.toString(old_price));
            return true;
        }
        else return false;
    }

    // Create a new car location or add cars to an existing location
    //  NOTE: if price <= 0 and the location already exists, it maintains its current price
    public boolean addCars(int id, String location, int count, int price)
        throws RemoteException
    {
        String s = "car-" + location;
        String key = s.toLowerCase();
        if (!txn_manager.requestLock(id, key, WRITE))
        {
            Trace.warn("RM::Lock failed--Can not acquire lock");
            return false;
        }
        int old_price = rm_car.queryCarsPrice(id, location);
        if (rm_car.addCars(id, location, count, price))
        {
            String command = "newcar";
            txn_manager.active_txn.get(id).addHistory(command, location, Integer.toString(count), Integer.toString(old_price));
            return true;
        }
        else return false;
    }


    // Delete cars from a location
    public boolean deleteCars(int id, String location)
        throws RemoteException
    {
        String s = "car-" + location;
        String key = s.toLowerCase();
        if (!txn_manager.requestLock(id, key, WRITE))
        {
            Trace.warn("RM::Lock failed--Can not acquire lock");
            return false;
        }
        //return rm_car.deleteCars(id, location);
        int old_price = rm_car.queryCarsPrice(id, location);
        int old_count = rm_car.queryCars(id, location);
        if (rm_car.deleteCars(id, location))
        {
            String command = "deletecar";
            txn_manager.active_txn.get(id).addHistory(command, location, Integer.toString(old_count), Integer.toString(old_price));
            return true;
        }
        else return false;
    }


    // Returns the number of empty seats on this flight
    public int queryFlight(int id, int flightNum)
        throws RemoteException
    {
        if (id == 0)
        {
            return rm_flight.queryFlight(id, flightNum);
        }
        String s = "flight-" + flightNum;
        String key = s.toLowerCase();
        if (!txn_manager.requestLock(id, key, READ))
        {
            Trace.warn("RM::Lock failed--Can not acquire lock");
            return -1;
        }
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
        if (id == 0)
        {
            return rm_flight.queryFlightPrice(id, flightNum);
        }
        String s = "flight-" + flightNum;
        String key = s.toLowerCase();
        if (!txn_manager.requestLock(id, key, READ))
        {
            Trace.warn("RM::Lock failed--Can not acquire lock");
            return -1;
        }
    	return rm_flight.queryFlightPrice(id, flightNum);
    }


    // Returns the number of rooms available at a location
    public int queryRooms(int id, String location)
        throws RemoteException
    {
        if (id == 0)
        {
            return rm_room.queryRooms(id, location);
        }
        String s = "room-" + location;
        String key = s.toLowerCase();
        if (!txn_manager.requestLock(id, key, READ))
        {
            Trace.warn("RM::Lock failed--Can not acquire lock");
            return -1;
        }
    	return rm_room.queryRooms(id, location);
    }


    
    // Returns room price at this location
    public int queryRoomsPrice(int id, String location)
        throws RemoteException
    {
        if (id == 0)
        {
            return rm_room.queryRoomsPrice(id, location);
        }
        String s = "room-" + location;
        String key = s.toLowerCase();
        if (!txn_manager.requestLock(id, key, READ))
        {
            Trace.warn("RM::Lock failed--Can not acquire lock");
            return -1;
        }
    	return rm_room.queryRoomsPrice(id, location);
    }


    // Returns the number of cars available at a location
    public int queryCars(int id, String location)
        throws RemoteException
    {
        if (id == 0) {
            return rm_car.queryCars(id, location);
        }
        String s = "car-" + location;
        String key = s.toLowerCase();
        if (!txn_manager.requestLock(id, key, READ))
        {
            Trace.warn("RM::Lock failed--Can not acquire lock");
            return -1;
        }
    	return rm_car.queryCars(id, location);
    }


    // Returns price of cars at this location
    public int queryCarsPrice(int id, String location)
        throws RemoteException
    {
        if (id == 0)
        {
            return rm_car.queryCarsPrice(id, location);
        }
        String s = "car-" + location;
        String key = s.toLowerCase();
        if (!txn_manager.requestLock(id, key, READ))
        {
            Trace.warn("RM::Lock failed--Can not acquire lock");
            return -1;
        }
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
        if (id == 0)
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
            }
        }
        Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + ") called" );
        String key = Customer.getKey(customerID);
        if (!txn_manager.requestLock(id, key, READ))
        {
            Trace.warn("RM::Lock failed--Can not acquire lock");
            return "";
        }
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        if ( cust == null ) {
            Trace.warn("RM::queryCustomerInfo(" + id + ", " + customerID + ") failed--customer doesn't exist" );
            return "";   // NOTE: don't change this--WC counts on this value indicating a customer does not exist...
        } else {
                String s = cust.printBill();
                Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + "), bill follows..." );
                System.out.println( s );
                return s;
        }
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
        if (!txn_manager.requestLock(id, cust.getKey(), WRITE))
        {
            Trace.warn("RM::Lock failed--Can not acquire lock");
            return -1;
        }
        writeData( id, cust.getKey(), cust );
        Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid );
        String command = "newcustomer";
        txn_manager.active_txn.get(id).addHistory(command, Integer.toString(cid));
        return cid;
    }

    // I opted to pass in customerID instead. This makes testing easier
    public boolean newCustomer(int id, int customerID )
        throws RemoteException
    {
        Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") called" );
        if (!txn_manager.requestLock(id, Customer.getKey(customerID), WRITE))
        {
            Trace.warn("RM::Lock failed--Can not acquire lock");
            return false;
        }
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        if ( cust == null ) {
            cust = new Customer(customerID);
            writeData( id, cust.getKey(), cust );
            Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") created a new customer" );
            String command = "newcustomer";
            txn_manager.active_txn.get(id).addHistory(command, Integer.toString(customerID));
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
        if (!txn_manager.requestLock(id, Customer.getKey(customerID), WRITE))
        {
            Trace.warn("RM::Lock failed--Can not acquire lock");
            return false;
        }
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        if ( cust == null ) {
            Trace.warn("RM::deleteCustomer(" + id + ", " + customerID + ") failed--customer doesn't exist" );
            return false;
        } else {            
            // Increase the reserved numbers of all reservable items which the customer reserved. 
            RMHashtable reservationHT = cust.getReservations();
            int i = 0;
            String[] reservedkey = new String[reservationHT.size()];
            ReservedItem[] reserveditem = new ReservedItem[reservationHT.size()];
            int[] reservedCount = new int[reservationHT.size()];
            String res_history = "";
            String command = "deletecustomer";
            txn_manager.active_txn.get(id).addHistory(command, Customer.getKey(customerID), res_history);
            for (Enumeration e = reservationHT.keys(); e.hasMoreElements();i++) {        
                reservedkey[i] = (String) (e.nextElement());
                reserveditem[i] = cust.getReservedItem(reservedkey[i]);
                reservedCount[i] = reserveditem[i].getCount();
                if (!txn_manager.requestLock(id, reservedkey[i], WRITE))
                {
                    Trace.warn("RM::Lock failed--Can not acquire lock");
                    return false;
                }
            }
            for (i = 0; i < reservationHT.size(); i++)
            {
                switch (reservedkey[i].charAt(0)) {
                    case 'c':
                        rm_car.freeItemRes(id, customerID, reservedkey[i], reservedCount[i]);
                        break;
                    case 'f':
                        rm_flight.freeItemRes(id, customerID, reservedkey[i], reservedCount[i]);
                        break;
                    case 'r':
                        rm_room.freeItemRes(id, customerID, reservedkey[i], reservedCount[i]);
                        break;
                    default:
                        break;
                }
                String sub_hist = reservedkey[i] + "," + Integer.toString(reservedCount[i]) + ";";
                txn_manager.active_txn.get(id).addSubHistory(sub_hist,2);
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
        String s = "car-" + location;
        String key = s.toLowerCase();
        if (!txn_manager.requestLock(id, Customer.getKey(customerID), WRITE) || !txn_manager.requestLock(id, key, WRITE))
        {
            Trace.warn("RM::Lock failed--Can not acquire lock");
            return false;
        }
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        // String key = ("car-" + location).toLowerCase();
        if ( cust == null ) {
            Trace.warn("RM::reserveCar( " + id + ", " + customerID + ", " + key + ", "+location+")  failed--customer doesn't exist" );
            return false;
        } else {
            if (rm_car.reserveCar(id, customerID, location) == true){
                cust.reserve( key, location, rm_car.queryCarsPrice(id, location));      
                writeData( id, cust.getKey(), cust );
                String command = "reservecar";
                txn_manager.active_txn.get(id).addHistory(command, Integer.toString(customerID), key, location);
                return true;
            } else {
                Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", " + key+", " + location+") failed" );
                return false;
            }
        }
    }

    // Adds room reservation to this customer. 
    public boolean reserveRoom(int id, int customerID, String location)
        throws RemoteException
    {
        String s = "room-" + location;
        String key = s.toLowerCase();
        if (!txn_manager.requestLock(id, Customer.getKey(customerID), WRITE) || !txn_manager.requestLock(id, key, WRITE))
        {
            Trace.warn("RM::Lock failed--Can not acquire lock");
            return false;
        }
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        // String key = ("room-" + location).toLowerCase();
        if ( cust == null ) {
            Trace.warn("RM::reserveRoom( " + id + ", " + customerID + ", " + key + ", "+location+")  failed--customer doesn't exist" );
            return false;
        } else {
            if(rm_room.reserveRoom(id, customerID, location) == true){
                cust.reserve( key, location, rm_room.queryRoomsPrice(id, location));
                writeData( id, cust.getKey(), cust );
                String command = "reserveroom";
                txn_manager.active_txn.get(id).addHistory(command, Integer.toString(customerID), key, location);
                return true;
            } else {
                Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", " + key+", " + location+") failed" );
                return false;
            }
        }
    }
    // Adds flight reservation to this customer.  
    public boolean reserveFlight(int id, int customerID, int flightNum)
        throws RemoteException
    {
        String s = "flight-" + flightNum;
        String key = s.toLowerCase();
        if (!txn_manager.requestLock(id, Customer.getKey(customerID), WRITE) || !txn_manager.requestLock(id, key, WRITE))
        {
            Trace.warn("RM::Lock failed--Can not acquire lock");
            return false;
        }
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        // String key = ("flight-" + flightNum).toLowerCase();

        if ( cust == null ) {
            Trace.warn("RM::reserveFlight( " + id + ", " + customerID + ", " + key + ", "+String.valueOf(flightNum)+")  failed--customer doesn't exist" );
            return false;
        } else {
            if(rm_flight.reserveFlight(id, customerID, flightNum) == true){
                cust.reserve( key, String.valueOf(flightNum), rm_flight.queryFlightPrice(id, flightNum));      
                writeData( id, cust.getKey(), cust );
                String command = "reserveflight";
                txn_manager.active_txn.get(id).addHistory(command, Integer.toString(customerID), key, Integer.toString(flightNum));
                return true;
            } else {
                Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", " + key+", " + flightNum+") failed" );
                return false;
            }
        }
    }

    public RMHashtable getCustomerReservations(int id, int customerID)
        throws RemoteException
    {
        Trace.info("RM::getCustomerReservations(" + id + ", " + customerID + ") called" );
        if (!txn_manager.requestLock(id, Customer.getKey(customerID), READ))
        {
            Trace.warn("RM::Lock failed--Can not acquire lock");
            return null;
        }
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        if ( cust == null ) {
            Trace.warn("RM::getCustomerReservations failed(" + id + ", " + customerID + ") failed--customer doesn't exist" );
            return null;
        } else {
            return cust.getReservations();
        } // if
    }
    // Reserve an itinerary 
    public boolean itinerary(int id,int customer,Vector flightNumbers,String location,boolean car,boolean room)
        throws RemoteException
    {
        if (flightNumbers.size()==0) {
            return false;
        }
        if (!txn_manager.requestLock(id, Customer.getKey(customer), WRITE))
        {
            Trace.warn("RM::Lock failed--Can not acquire lock");
            return false;
        }
        Customer cust = (Customer) readData( id, Customer.getKey(customer) );        
        if ( cust == null ) {
            return false;
        }
        String res_history = "";
        String cust_history = "";
        String command = "itinerary";
        txn_manager.active_txn.get(id).addHistory(command, Integer.toString(customer), res_history, cust_history);
        // Hashtable<Integer,Integer> f_cnt = new Hashtable<Integer,Integer>();
        // int[] flights = new int[flightNumbers.size()];
        // for (int i = 0; i < flightNumbers.size(); i++) {
        //     try {
        //         flights[i] = gi(flightNumbers.elementAt(i));
        //     }
        //     catch (Exception e){}
        // }
        // for (int i = 0; i < flightNumbers.size(); i++) {
        //     if (f_cnt.containsKey(flights[i]))
        //         f_cnt.put(flights[i], f_cnt.get(flights[i])+1);
        //     else
        //         f_cnt.put(flights[i], 1);
        // }

        // if (car) {
            // res_history += "car-" + location + ",1;";
            // // check if the item is available
            // int item = rm_car.queryCars(id, location);
            // if ( item == 0 )
            //     return false;
        // }

        // if (room) {
            // res_history += "room-" + location + ",1;";
            // // check if the item is available
            // int item = rm_room.queryRooms(id, location);
            // if ( item == 0 )
            //     return false;
        // }
        // Set<Integer> keys = f_cnt.keySet();
        // for (int key : keys) {
            // res_history += "flight-" + key + "," + f_cnt.get(key) + ";";
            // int item = rm_flight.queryFlight(id, key);
            // if (item < f_cnt.get(key))
            //     return false;
        // }
        String car_key = ("car-" + location).toLowerCase();
        if (!txn_manager.requestLock(id, car_key, WRITE))
        {
            Trace.warn("RM::Lock failed--Can not acquire lock");
            return false;
        }
        String room_key = ("room-" + location).toLowerCase();
        if (!txn_manager.requestLock(id, room_key, WRITE))
        {
            Trace.warn("RM::Lock failed--Can not acquire lock");
            return false;
        }
        boolean car_reserved = false;
        boolean room_reserved = false;
        String[] flight_key = new String[flightNumbers.size()];
        boolean[] flight_reserved = new boolean[flightNumbers.size()];
        for (int i = 0; i < flightNumbers.size(); i++ ) {
            int flightNum = Integer.parseInt((String)flightNumbers.elementAt(i));
            flight_key[i] = ("flight-" + flightNum).toLowerCase();
            if (!txn_manager.requestLock(id, flight_key[i], WRITE))
            {
                Trace.warn("RM::Lock failed--Can not acquire lock");
                return false;
            }
            flight_reserved[i] = false;
        }
        if (car) {
            car_reserved = rm_car.reserveCar(id, customer, location);
            txn_manager.active_txn.get(id).addSubHistory(car_key+",1;",2);
            if (!car_reserved) {
                txn_manager.active_txn.get(id).pop();
                return false;
            }
        }
        if (room) {
            room_reserved = rm_room.reserveRoom(id, customer, location);
            txn_manager.active_txn.get(id).addSubHistory(room_key+",1;",2);
            if (!room_reserved) {
                if (car_reserved) {
                    rm_car.freeItemRes(id, customer, car_key, 1);
                }
                txn_manager.active_txn.get(id).pop();
                return false;
            }
        }
        for (int i = 0; i < flightNumbers.size(); i++ ) {
            flight_reserved[i] = rm_flight.reserveFlight(id, customer, Integer.parseInt((String)flightNumbers.elementAt(i)));
            txn_manager.active_txn.get(id).addSubHistory(flight_key[i]+",1;",2);
            if (!flight_reserved[i]) {
                if (car_reserved) {
                    rm_car.freeItemRes(id, customer, car_key, 1);
                }
                if (room_reserved) {
                    rm_room.freeItemRes(id, customer, room_key, 1);
                }
                for (int j = 0; j < i; j++ ) {
                    rm_flight.freeItemRes(id, customer, flight_key[j], 1);
                }
                txn_manager.active_txn.get(id).pop();
                return false;
            }
        }
        if (car_reserved) {
            cust.reserve( car_key, location, rm_car.queryCarsPrice(id, location));
            txn_manager.active_txn.get(id).addSubHistory(car_key+",1;",3);
            writeData( id, cust.getKey(), cust );
        }
        if (room_reserved) {
            cust.reserve( room_key, location, rm_room.queryRoomsPrice(id, location));
            txn_manager.active_txn.get(id).addSubHistory(room_key+",1;",3);
            writeData( id, cust.getKey(), cust );
        }
        for (int i = 0; i < flightNumbers.size(); i++ ) {
            int flightNum = Integer.parseInt((String)flightNumbers.elementAt(i));
            cust.reserve( flight_key[i], String.valueOf(flightNum), rm_flight.queryFlightPrice(id, flightNum));
            txn_manager.active_txn.get(id).addSubHistory(flight_key[i]+",1;",3);
            writeData( id, cust.getKey(), cust );
        }
        return true;
    }
    // Convert Object to int
    public int gi(Object temp) throws Exception {
        try {
            return (new Integer((String)temp)).intValue();
        }
        catch(Exception e) {
            throw e;
        }
    }
    // Convert Object to boolean
    public boolean gb(Object temp) throws Exception {
        try {
            return (new Boolean((String)temp)).booleanValue();
            }
        catch(Exception e) {
            throw e;
            }
    }
    // Convert Object to String
    public String gs(Object temp) throws Exception {
        try {    
            return (String)temp;
            }
        catch (Exception e) {
            throw e;
            }
    }

    public boolean reserveItinerary(int id,int customer,Vector flightNumbers,String location, boolean Car, boolean Room)
        throws RemoteException
    {
        return false;
    }

    public int start() throws RemoteException {
        return txn_manager.start();
    }

    public boolean commit(int transactionId) 
        throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
         return txn_manager.commit(transactionId);
    }

    public void abort(int transactionId) throws RemoteException, InvalidTransactionException
    {
        Stack<Vector> history = txn_manager.getHistory(transactionId);
        while (!history.empty())
        {
            Vector<String> v = history.pop();
            if (v.get(0).equals("newcar"))
            {
                rm_car.removeCars(transactionId, v.get(1), v.get(2), v.get(3));
            }
            else if (v.get(0).equals("deletecar"))
            {
                rm_car.recoverCars(transactionId, v.get(1), v.get(2), v.get(3));
            }
            else if (v.get(0).equals("reservecar"))
            {
                int customer = Integer.parseInt(v.get(1));
                rm_car.freeItemRes(transactionId, customer, v.get(2), 1);
                Customer cust = (Customer) readData( transactionId, Customer.getKey(customer));
                cust.cancel(v.get(2), v.get(3),1);
                writeData(transactionId, cust.getKey(), cust);
            }
            else if (v.get(0).equals("newroom"))
            {
                rm_room.removeRooms(transactionId, v.get(1), v.get(2), v.get(3));
            }
            else if (v.get(0).equals("deleteroom"))
            {
                rm_room.recoverRooms(transactionId, v.get(1), v.get(2), v.get(3));
            }
            else if (v.get(0).equals("reserveroom"))
            {
                int customer = Integer.parseInt(v.get(1));
                rm_room.freeItemRes(transactionId, customer, v.get(2), 1);
                Customer cust = (Customer) readData( transactionId, Customer.getKey(customer));
                cust.cancel(v.get(2), v.get(3),1);
                writeData(transactionId, cust.getKey(), cust);
            }
            else if (v.get(0).equals("newflight"))
            {
                rm_flight.removeFlight(transactionId, v.get(1), v.get(2), v.get(3));
            }
            else if (v.get(0).equals("deleteflight"))
            {
                rm_flight.recoverFlight(transactionId, v.get(1), v.get(2), v.get(3));
            }
            else if (v.get(0).equals("reserveflight"))
            {
                int customer = Integer.parseInt(v.get(1));
                rm_flight.freeItemRes(transactionId, customer, v.get(2), 1);
                Customer cust = (Customer) readData( transactionId, Customer.getKey(customer));
                cust.cancel(v.get(2), v.get(3),1);
                writeData(transactionId, cust.getKey(), cust);
            }
            else if (v.get(0).equals("newcustomer"))
            {
                removeData(transactionId, Customer.getKey(Integer.parseInt(v.get(1))));
            }
            else if (v.get(0).equals("deletecustomer"))
            {
                int customerID = Integer.parseInt(v.get(1));
                Customer cust = new Customer(customerID);
                String res_history = v.get(2);
                String[] reserved = res_history.split(";");
                for (int i = 0; i < reserved.length; i++)
                {
                    String[] tokens = reserved[i].split(",");
                    int count = Integer.parseInt(tokens[1]);
                    String[] keys = tokens[0].split("-");
                    switch (tokens[0].charAt(0)) {
                        case 'c':
                            rm_car.undoFreeItemRes(transactionId, customerID, tokens[0], count);
                            cust.reserve(tokens[0], keys[1], rm_car.queryCarsPrice(transactionId, keys[1]), count);
                            break;
                        case 'f':
                            rm_flight.undoFreeItemRes(transactionId, customerID, tokens[0],count);
                            cust.reserve(tokens[0], keys[1], rm_flight.queryFlightPrice(transactionId, Integer.parseInt(keys[1])), count);
                            break;
                        case 'r':
                            rm_room.undoFreeItemRes(transactionId, customerID, tokens[0], count);
                            cust.reserve(tokens[0], keys[1], rm_room.queryRoomsPrice(transactionId, keys[1]), count);
                            break;
                        default:
                            break;
                    }
                }
                writeData(transactionId, cust.getKey(), cust);
            }
            else if (v.get(0).equals("itinerary"))
            {
                int customerID = Integer.parseInt(v.get(1));
                Customer cust = (Customer) readData( transactionId, Customer.getKey(customerID));
                String res_history = v.get(2);
                String cust_history = v.get(3);
                String[] reserved = res_history.split(";");
                for (int i = 0; i < reserved.length; i++)
                {
                    String[] tokens = reserved[i].split(",");
                    int count = Integer.parseInt(tokens[1]);
                    String[] keys = tokens[0].split("-");
                    switch (tokens[0].charAt(0)) {
                        case 'c':
                            rm_car.freeItemRes(transactionId, customerID, tokens[0], count);
                            break;
                        case 'f':
                            rm_flight.freeItemRes(transactionId, customerID, tokens[0], count);
                            break;
                        case 'r':
                            rm_room.freeItemRes(transactionId, customerID, tokens[0], count);
                            break;
                        default:
                            break;
                    }
                }
                String[] cust_reserved = cust_history.split(";");
                for (int i = 0; i < cust_reserved.length; i++)
                {
                    String[] tokens = cust_reserved[i].split(",");
                    int count = Integer.parseInt(tokens[1]);
                    String[] keys = tokens[0].split("-");
                    cust.cancel(tokens[0], keys[1], count);
                }
                writeData(transactionId, cust.getKey(), cust);
            }
        }
        txn_manager.abort(transactionId);
    }

    public boolean shutdown() throws RemoteException
    {
        if (txn_manager.shutdown()) {
            Trace.warn("MW::Shutting down FlightRM ...");
            rm_flight.shutdown();
            Trace.warn("MW::Shutting down CarRM ...");
            rm_car.shutdown();
            Trace.warn("MW::Shutting down RoomRm ...");
            rm_room.shutdown();
            Trace.warn("MW::Shutting down middleware ...");
            Registry registry = LocateRegistry.getRegistry(1088);
            try {
              registry.unbind("TripMiddleWare");
              UnicastRemoteObject.unexportObject(this, false);
            } catch (Exception e) {
              throw new RemoteException("Could not unregister middleware, quiting anyway", e);
            }
          
            new Thread() {
              @Override
              public void run() {
                System.out.print("Shutting down...");
                try {
                  sleep(1000);
                } catch (InterruptedException e) {
                  // I don't care
                }
                System.out.println("done");
                System.exit(0);
              }
          
            }.start();
            return true;
        }
        else {
            Trace.warn("MW::Shutdown failed");
            return false;
        }
    }
}