package MidImpl;
import ResInterface.*;
import MidInterface.*;
import LockManager.*;


import MidImpl.IOTools;
import MidImpl.CrashException;
import java.util.*;
import java.io.File;
import java.io.Serializable;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RMISecurityManager;

public class MiddleWareImpl implements MiddleWare, Serializable
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
    
    protected Hashtable<Integer,Transaction> active_txn = new Hashtable<Integer, Transaction>();
    protected Hashtable<Integer,LogFile> active_log = new Hashtable<Integer, LogFile>();
    
    protected RMHashtable m_itemHT = new RMHashtable();
    protected MasterRecord master = new MasterRecord();
    protected static String rm_name = "TripMiddleWare";
    protected static String mr_fname = "";
    protected static String shadow_fname = "";
    // protected static String shadow_tm_fname = "";
    protected static String ws_fname = "";
    protected int crash_mode = 0;
    protected static String server_flight = "localhost";
    protected static String server_car = "localhost";
    protected static String server_room = "localhost";
    public String customerRM = "CustomerRM";

    protected static int port_local = 1088;
    protected static int port = 2199;

    public static void main(String args[]) {

        if (args.length == 3) {
            server_flight = args[0];
            server_car = args[1];
            server_room = args[2];
            mr_fname = "" + rm_name + "_MasterRecord.txt";
            ws_fname = "" + rm_name + "_WS_";
            shadow_fname = "" + rm_name + "_Shadow_";
            // shadow_tm_fname = "" + rm_name + "_Shadow_TM_";
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
            File file = new File("TransactionManager.txt");
            if (file.exists()) {
                obj.txn_manager = (TransactionManager) IOTools.loadFromDisk("TransactionManager.txt");
                System.out.println("Transaction manager loaded.");
                obj.txn_manager.setMW(obj);
                // obj.recoverTransactionManagerStatus();                
                obj.recoverCustomerRMStatus();
            }
            else {
                obj.txn_manager = new TransactionManager(mw, rm_flight, rm_car, rm_room);
            }
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
        File master_file = new File(mr_fname);
        if (master_file.exists()) {
            System.out.println("Master Record exists, loading from disk ......");
            this.master = (MasterRecord) IOTools.loadFromDisk(mr_fname);
            System.out.println("Master Record loaded.");
            this.m_itemHT = (RMHashtable) IOTools.loadFromDisk(shadow_fname + Integer.toString(master.getCommittedIndex()) + ".txt");
            System.out.println("Data hashtable loaded.");
        }
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
        return rm_flight.addFlight(id,flightNum,flightSeats,flightPrice);
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
        return rm_flight.deleteFlight(id, flightNum);
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
        return rm_room.addRooms(id, location, count, price);
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
        
        if (rm_room.deleteRooms(id, location))
        {
            
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
        return rm_car.addCars(id, location, count, price);
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
        return rm_car.deleteCars(id, location);
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
        this.active_txn.get(id).addHistory(command, Integer.toString(cid));
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
            this.active_txn.get(id).addHistory(command, Integer.toString(customerID));
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
            this.active_txn.get(id).addHistory(command, Integer.toString(customerID), res_history);
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
                this.active_txn.get(id).addSubHistory(sub_hist,2);
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
                this.active_txn.get(id).addHistory(command, Integer.toString(customerID), key, location);
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
                this.active_txn.get(id).addHistory(command, Integer.toString(customerID), key, location);
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
                this.active_txn.get(id).addHistory(command, Integer.toString(customerID), key, Integer.toString(flightNum));
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
        //     res_history += "car-" + location + ",1;";
        //     // check if the item is available
        //     int item = rm_car.queryCars(id, location);
        //     if ( item == 0 )
        //         return false;
        // }

        // if (room) {
        //     res_history += "room-" + location + ",1;";
        //     // check if the item is available
        //     int item = rm_room.queryRooms(id, location);
        //     if ( item == 0 )
        //         return false;
        // }
        // Set<Integer> keys = f_cnt.keySet();
        // for (int key : keys) {
        //     res_history += "flight-" + key + "," + f_cnt.get(key) + ";";
        //     int item = rm_flight.queryFlight(id, key);
        //     if (item < f_cnt.get(key))
        //         return false;
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
        String res_history = "";
        String cust_history = "";
        String command = "itinerary";
        this.active_txn.get(id).addHistory(command, Integer.toString(customer), res_history, cust_history);
        if (car) {
            car_reserved = rm_car.reserveCar(id, customer, location);
            this.active_txn.get(id).addSubHistory(car_key+",1;",2);
            if (!car_reserved) {
                this.active_txn.get(id).pop();
                return false;
            }
        }
        if (room) {
            room_reserved = rm_room.reserveRoom(id, customer, location);
            this.active_txn.get(id).addSubHistory(room_key+",1;",2);
            if (!room_reserved) {
                if (car_reserved) {
                    rm_car.freeItemRes(id, customer, car_key, 1, true);
                    rm_car.pop(id);
                }
                this.active_txn.get(id).pop();
                return false;
            }
        }
        for (int i = 0; i < flightNumbers.size(); i++ ) {
            flight_reserved[i] = rm_flight.reserveFlight(id, customer, Integer.parseInt((String)flightNumbers.elementAt(i)));
            this.active_txn.get(id).addSubHistory(flight_key[i]+",1;",2);
            if (!flight_reserved[i]) {
                if (car_reserved) {
                    rm_car.freeItemRes(id, customer, car_key, 1, true);
                    rm_car.pop(id);
                }
                if (room_reserved) {
                    rm_room.freeItemRes(id, customer, room_key, 1, true);
                    rm_room.pop(id);
                }
                for (int j = 0; j < i; j++ ) {
                    rm_flight.freeItemRes(id, customer, flight_key[j], 1, true);
                    rm_flight.pop(id);
                }
                this.active_txn.get(id).pop();
                return false;
            }
        }
        if (car_reserved) {
            cust.reserve( car_key, location, rm_car.queryCarsPrice(id, location));
            this.active_txn.get(id).addSubHistory(car_key+",1;",3);
            writeData( id, cust.getKey(), cust );
        }
        if (room_reserved) {
            cust.reserve( room_key, location, rm_room.queryRoomsPrice(id, location));
            this.active_txn.get(id).addSubHistory(room_key+",1;",3);
            writeData( id, cust.getKey(), cust );
        }
        for (int i = 0; i < flightNumbers.size(); i++ ) {
            int flightNum = Integer.parseInt((String)flightNumbers.elementAt(i));
            cust.reserve( flight_key[i], String.valueOf(flightNum), rm_flight.queryFlightPrice(id, flightNum));
            this.active_txn.get(id).addSubHistory(flight_key[i]+",1;",3);
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
    public int start(int transactionId) throws RemoteException
    {
        synchronized(this.active_txn) {
            Transaction txn = new Transaction(transactionId);
            this.active_txn.put(transactionId, txn);
        }
        LogFile log = new LogFile(transactionId);
        this.active_log.put(transactionId, log);
        return transactionId;
    }


    public int prepare(int transactionId)
        throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {   
        String record = "BEFORE_YES";
        this.active_log.get(transactionId).record.add(record);
        IOTools.saveToDisk(this.active_log.get(transactionId), customerRM + "_" + Integer.toString(transactionId) + ".log");
        if (crash_mode == 1) return (selfDestruct(crash_mode)) ? 1 : 0;        
        IOTools.saveToDisk(active_txn, ws_fname + Integer.toString(transactionId) + ".txt");
        record = "AFTER_YES";
        this.active_log.get(transactionId).record.add(record);
        IOTools.saveToDisk(this.active_log.get(transactionId), customerRM + "_" + Integer.toString(transactionId) + ".log");
        return 1;
    }

    public boolean commit(int transactionId) 
        throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
         return txn_manager.commit(transactionId);
    }

    public boolean local_commit(int transactionId)
        throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        synchronized(this.active_txn) {
            if (transactionId < 1 || !this.active_txn.containsKey(transactionId)) {
                Trace.warn("RM::Commit failed--Invalid transactionId");
                throw new InvalidTransactionException(transactionId);
            }
            else
            {
                Trace.info("RM::Committing transaction : " + transactionId);
                String record = "BEFORE_COMMIT";
                this.active_log.get(transactionId).record.add(record);
                IOTools.saveToDisk(this.active_log.get(transactionId), customerRM + "_"+ Integer.toString(transactionId) + ".log");  
                if (crash_mode == 2) return selfDestruct(crash_mode);                
                IOTools.saveToDisk(m_itemHT, shadow_fname + Integer.toString(master.getWorkingIndex()) + ".txt");
                master.setLastXid(transactionId);
                master.swap();
                IOTools.saveToDisk(master, mr_fname);

                record = "AFTER_COMMIT";
                this.active_log.get(transactionId).record.add(record);
                IOTools.saveToDisk(this.active_log.get(transactionId), customerRM + "_" + Integer.toString(transactionId) + ".log");                
                IOTools.deleteFile(ws_fname + Integer.toString(transactionId) + ".txt");
                this.active_txn.remove(transactionId);
                IOTools.deleteFile(rm_name + "_" + Integer.toString(transactionId) + ".log");
                this.active_log.remove(transactionId);
                if (crash_mode == 3) return selfDestruct(crash_mode);
            }
        }
        return true;
    }

    public Stack getHistory(int transactionId)
    {
        synchronized(this.active_txn) {
            Transaction t = this.active_txn.get(transactionId);
            if (t!= null) return t.txn_hist;
            else return null;
        }
    }

    public void abort(int transactionId) throws RemoteException, InvalidTransactionException
    {
        txn_manager.abort(transactionId);
    }

    public void local_abort(int transactionId) throws RemoteException, InvalidTransactionException
    {
        Stack<Vector> history = getHistory(transactionId);
        if (history == null) return;
        String record = "BEFORE_ABORT";
        this.active_log.get(transactionId).record.add(record);
        IOTools.saveToDisk(this.active_log.get(transactionId), customerRM + "_"+ Integer.toString(transactionId) + ".log");
        if (crash_mode == 2) 
        {
            selfDestruct(crash_mode);
            return;
        }
        while (!history.empty())
        {
            Vector<String> v = history.pop();
            if (v.get(0).equals("reservecar"))
            {
                int customer = Integer.parseInt(v.get(1));
                // rm_car.freeItemRes(transactionId, customer, v.get(2), 1);
                Customer cust = (Customer) readData( transactionId, Customer.getKey(customer));
                cust.cancel(v.get(2), v.get(3),1);
                writeData(transactionId, cust.getKey(), cust);
            }
            else if (v.get(0).equals("reserveroom"))
            {
                int customer = Integer.parseInt(v.get(1));
                // rm_room.freeItemRes(transactionId, customer, v.get(2), 1);
                Customer cust = (Customer) readData( transactionId, Customer.getKey(customer));
                cust.cancel(v.get(2), v.get(3),1);
                writeData(transactionId, cust.getKey(), cust);
            }
            else if (v.get(0).equals("reserveflight"))
            {
                int customer = Integer.parseInt(v.get(1));
                // rm_flight.freeItemRes(transactionId, customer, v.get(2), 1);
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
                            // rm_car.undoFreeItemRes(transactionId, customerID, tokens[0], count);
                            cust.reserve(tokens[0], keys[1], rm_car.queryCarsPrice(transactionId, keys[1]), count);
                            break;
                        case 'f':
                            // rm_flight.undoFreeItemRes(transactionId, customerID, tokens[0],count);
                            cust.reserve(tokens[0], keys[1], rm_flight.queryFlightPrice(transactionId, Integer.parseInt(keys[1])), count);
                            break;
                        case 'r':
                            // rm_room.undoFreeItemRes(transactionId, customerID, tokens[0], count);
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
                // String res_history = v.get(2);
                String cust_history = v.get(3);
                // String[] reserved = res_history.split(";");
                // for (int i = 0; i < reserved.length; i++)
                // {
                //     String[] tokens = reserved[i].split(",");
                //     int count = Integer.parseInt(tokens[1]);
                //     String[] keys = tokens[0].split("-");
                //     switch (tokens[0].charAt(0)) {
                //         case 'c':
                //             rm_car.freeItemRes(transactionId, customerID, tokens[0], count);
                //             break;
                //         case 'f':
                //             rm_flight.freeItemRes(transactionId, customerID, tokens[0], count);
                //             break;
                //         case 'r':
                //             rm_room.freeItemRes(transactionId, customerID, tokens[0], count);
                //             break;
                //         default:
                //             break;
                //     }
                // }
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
        record = "AFTER_ABORT";
        this.active_log.get(transactionId).record.add(record);
        IOTools.saveToDisk(this.active_log.get(transactionId), customerRM + "_"+ Integer.toString(transactionId) + ".log");
        IOTools.deleteFile(ws_fname + Integer.toString(transactionId) + ".txt");
        this.active_txn.remove(transactionId);
        IOTools.deleteFile(rm_name + "_" + Integer.toString(transactionId) + ".log");
        this.active_log.remove(transactionId);
        if (crash_mode == 3) 
        {
            selfDestruct(crash_mode);
            return;
        }
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

    public void setCrashMode(String which, int mode) throws RemoteException
    {
        if (mode < 0 || mode > 10) return;
        switch (which.charAt(0)) {
            case 'c':
                rm_car.setCrashMode(mode);
                break;
            case 'f':
                rm_flight.setCrashMode(mode);
                break;
            case 'r':
                rm_room.setCrashMode(mode);
                break;
            case 't':
                txn_manager.setCrashMode(mode);
                break;
            case 'm':
                crash_mode = mode;
                break;
            default:
                break;  
        }
    }

    private boolean selfDestruct(int mode)
    {
        new Thread() {
            @Override
            public void run() {
                System.out.println("Self Destructing ...");
            //   try {
            //     // sleep(1000);
            //   } catch (InterruptedException e) {
            //     // I don't care
            //   }
                System.out.println("done");
                System.exit(mode);
            }
        
            }.start();
            return false;
    }

    public void buildLink(String rm_name) throws RemoteException
    {
        try {
            if (rm_name.equals("FlightRM"))
            {
                Registry registry_flight = LocateRegistry.getRegistry(server_flight, port);
                this.rm_flight = (ResourceManager) registry_flight.lookup("FlightRM");
                this.txn_manager.setFlightRM(this.rm_flight);
            }
            else if (rm_name.equals("CarRM"))
            {
                Registry registry_car = LocateRegistry.getRegistry(server_car, port);
                this.rm_car = (ResourceManager) registry_car.lookup("CarRM");
                this.txn_manager.setCarRM(this.rm_car);
            }
            else if (rm_name.equals("RoomRM"))
            {
                Registry registry_room = LocateRegistry.getRegistry(server_room, port);
                this.rm_room = (ResourceManager) registry_room.lookup("RoomRM");
                this.txn_manager.setRoomRM(this.rm_room);
            }
            else System.out.println("buildLink failed with wrong inputs");
        } catch (Exception e) {
            System.err.println("MiddleWare Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public boolean get_votes_result(int transactionId) throws RemoteException {
        return this.txn_manager.all_vote_yes.get(transactionId);
    }

    public void removeTransactionId(int transactionId) {
        this.txn_manager.active_txn.remove(transactionId);
    }

    public int getTransactionId(String filename) {
        return filename.charAt(filename.length()-5) - '0';
    }

    public void recoverTransactionManagerStatus() {
        File folder = new File(".");
        for (File f: folder.listFiles()) {
            try{
                String filename = f.getName();
                if (null != filename) {

                }  
            } catch (Exception e) {
                System.out.println(e.getMessage());                
            }
        }
    }

    public void recoverCustomerRMStatus() {
        File folder = new File(".");
        for (File f: folder.listFiles()) {
            try {
                String filename = f.getName();
                if (null != filename) {
                    if (filename.startsWith("Customer") && filename.endsWith(".log")) {
                        int transactionId = getTransactionId(filename);
                        File file = new File("TripMiddleWare_WS_" + transactionId + ".txt");
                        if (file.exists()) this.active_txn = (Hashtable) IOTools.loadFromDisk("TripMiddleWare_WS_" + transactionId + ".txt");
                        else this.active_txn = new Hashtable<Integer, Transaction>();
                        LogFile log = (LogFile) IOTools.loadFromDisk(filename);
                        this.active_log.put(transactionId, log);
                        if (log.record.contains("BEFORE_ABORT") || log.record.contains("AFTER_ABORT")) {
                            IOTools.deleteFile("TripMiddleWare_WS_" + Integer.toString(transactionId) + ".txt");
                            IOTools.deleteFile("CustomerRM" + "_" + Integer.toString(transactionId) + ".log");
                            this.active_log.remove(transactionId);
                            this.active_txn.remove(transactionId);                            
                            break;
                        }

                        if (transactionId < 1 || (!this.active_txn.containsKey(transactionId)&&this.active_txn.size()!=0)) {
                            throw new InvalidTransactionException(transactionId);
                        }
                        else
                        {
                            if (log.record.size() == 4) {
                                //Do nothing   
                            } else if (log.record.size() == 3 || log.record.size() == 2) {
                                if (this.get_votes_result(transactionId) == true) {
                                    this.recover_history(transactionId);
                                } else this.commit_no_crash(transactionId);
                            } else if (log.record.size() == 1 || log.record.size() == 0) {
                                IOTools.deleteFile("TripMiddleWare_WS_" + Integer.toString(transactionId) + ".txt");
                                IOTools.deleteFile("CustomerRM" + "_" + Integer.toString(transactionId) + ".log");
                                this.active_log.remove(transactionId);
                                this.active_txn.remove(transactionId);
                                this.removeTransactionId(transactionId);
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                System.out.println(e.getMessage()); 
            }
        }
    public boolean recover_history(int transactionId) throws RemoteException
    {
        Stack<Vector> tmp = getHistory(transactionId);
        if (tmp == null) return false;
        Stack<Vector> history = new Stack<Vector>();
        while (!tmp.empty())
        {
            history.push(tmp.pop());
        }
        while (!history.empty())
        {
            Vector<String> v = history.pop();
            if (v.get(0).equals("reservecar"))
            {
                int customer = Integer.parseInt(v.get(1));
                Customer cust = (Customer) readData( transactionId, Customer.getKey(customer));
                cust.reserve(v.get(2), v.get(3),1);
                writeData(transactionId, cust.getKey(), cust);
            }
            else if (v.get(0).equals("reserveroom"))
            {
                int customer = Integer.parseInt(v.get(1));
                Customer cust = (Customer) readData( transactionId, Customer.getKey(customer));
                cust.reserve(v.get(2), v.get(3),1);
                writeData(transactionId, cust.getKey(), cust);
            }
            else if (v.get(0).equals("reserveflight"))
            {
                int customer = Integer.parseInt(v.get(1));
                Customer cust = (Customer) readData( transactionId, Customer.getKey(customer));
                cust.reserve(v.get(2), v.get(3),1);
                writeData(transactionId, cust.getKey(), cust);
            }
            else if (v.get(0).equals("newcustomer"))
            {
                // removeData(transactionId, Customer.getKey(Integer.parseInt(v.get(1))));
                Customer cust = new Customer(Integer.parseInt(v.get(1)));
                writeData(transactionId, cust.getKey(), cust);                
            }
            else if (v.get(0).equals("deletecustomer"))
            {
                this.deleteCustomer(transactionId, Integer.parseInt(v.get(1)));
                this.active_txn.get(transactionId).pop();
            }
            else if (v.get(0).equals("itinerary"))
            {
                int customerID = Integer.parseInt(v.get(1));
                Customer cust = (Customer) readData( transactionId, Customer.getKey(customerID));
                // String res_history = v.get(2);
                String cust_history = v.get(3);
                String[] cust_reserved = cust_history.split(";");
                for (int i = 0; i < cust_reserved.length; i++)
                {
                    String[] tokens = cust_reserved[i].split(",");
                    int count = Integer.parseInt(tokens[1]);
                    String[] keys = tokens[0].split("-");
                    cust.reserve(tokens[0], keys[1], count);
                }
                writeData(transactionId, cust.getKey(), cust);
            }
        }
        try {
            return commit_no_crash(transactionId);
        }
        catch (Exception e) {return false;}
    }

    public boolean commit_no_crash(int transactionId) throws RemoteException, InvalidTransactionException
    {
        synchronized(this.active_txn) {
            if (transactionId < 1 || !this.active_txn.containsKey(transactionId)) {
                Trace.warn("RM::Commit failed--Invalid transactionId");
                throw new InvalidTransactionException(transactionId);
            }
            else
            {
                Trace.info("RM::Committing transaction : " + transactionId);
                IOTools.saveToDisk(m_itemHT, shadow_fname + Integer.toString(master.getWorkingIndex()) + ".txt");
                master.setLastXid(transactionId);
                master.swap();
                IOTools.saveToDisk(master, mr_fname);
                
                IOTools.deleteFile(ws_fname + Integer.toString(transactionId) + ".txt");
                this.active_txn.remove(transactionId);
                IOTools.deleteFile(rm_name + "_" + Integer.toString(transactionId) + ".log");
                this.active_log.remove(transactionId);
            }
        }
        return true;
    }
}