// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
//
package ResImpl;

import ResInterface.*;

import java.util.*;

import ResImpl.InvalidTransactionException;
import ResImpl.LogFile;
import MidInterface.MiddleWare;
import ResImpl.IOTools;
import ResImpl.CrashException;
import java.io.File;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RMISecurityManager;

public class ResourceManagerImpl implements ResourceManager 
{
    protected RMHashtable m_itemHT = new RMHashtable();
    protected Hashtable<Integer,Transaction> active_txn = new Hashtable<Integer, Transaction>();
    protected Hashtable<Integer,LogFile> active_log = new Hashtable<Integer, LogFile>();
    protected MasterRecord master = new MasterRecord();
    // protected LogFile log = new LogFile();
    protected static String rm_name = "name";
    protected static String mw_name = "TripMiddleWare";
    protected static String mr_fname = "";
    protected static String shadow_fname = "";
    protected static String ws_fname = "";
    protected int crash_mode = 0;
    public static Registry registry;
    private static MiddleWare mw = null;

    public static void main(String args[]) {
        // Figure out where server is running
        String server = "localhost";
        String mw_host = "localhost";
        int port = 2199;
        int mw_port = 1088;
        if (args.length == 2) {
            server = server + ":" + args[0];
            port = Integer.parseInt(args[0]);
            rm_name = args[1];
            mr_fname = "" + rm_name + "_MasterRecord.txt";
            ws_fname = "" + rm_name + "_WS_";
            shadow_fname = "" + rm_name + "_Shadow_";
        } else if (args.length == 4) {
            server = server + ":" + args[0];
            port = Integer.parseInt(args[0]);
            rm_name = args[1];
            mw_host = args[2];
            mw_port = Integer.parseInt(args[3]);
            mr_fname = "" + rm_name + "_MasterRecord.txt";
            ws_fname = "" + rm_name + "_WS_";
            shadow_fname = "" + rm_name + "_Shadow_";
            try {
                Registry mw_registry = LocateRegistry.getRegistry(mw_host, mw_port);
                mw = (MiddleWare) mw_registry.lookup(mw_name);
                if (mw != null)
                {
                    System.out.println("Successfully connected to the middleware");
                }
                else
                {
                    System.out.println("Connect to the middleware failed");
                    System.exit(1);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        } else  {
            System.err.println ("Wrong usage");
            System.out.println("Usage: java ResImpl.ResourceManagerImpl [port] [RM_NAME]");
            System.exit(1);
        }

        try {
            // create a new Server object
            ResourceManagerImpl obj = new ResourceManagerImpl();
            // dynamically generate the stub (client proxy)
            ResourceManager rm = (ResourceManager) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            registry = LocateRegistry.getRegistry(port);
            registry.rebind(rm_name, rm);

            if (mw != null){
                mw.buildLink(rm_name);
                System.out.println("RMI link rebuilt to Middleware");
                // TODO: recovery
                obj.recoverResourceManagerStatus();
                System.out.println(rm_name + " data recovered");
            }
            System.err.println(rm_name + " server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
    }
     
    public ResourceManagerImpl() throws RemoteException {
        File master_file = new File(mr_fname);
        if (master_file.exists()) {
            System.out.println("Master Record exists, loading from disk ......");
            this.master = (MasterRecord) IOTools.loadFromDisk(mr_fname);
            System.out.println("Master Record loaded.");
            m_itemHT = (RMHashtable) IOTools.loadFromDisk(shadow_fname + Integer.toString(master.getCommittedIndex()) + ".txt");
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
    
    
    // deletes the entire item
    protected boolean deleteItem(int id, String key)
    {
        Trace.info("RM::deleteItem(" + id + ", " + key + ") called" );
        ReservableItem curObj = (ReservableItem) readData( id, key );
        // Check if there is such an item in the storage
        if ( curObj == null ) {
            Trace.warn("RM::deleteItem(" + id + ", " + key + ") failed--item doesn't exist" );
            return false;
        } else {
            if (curObj.getReserved()==0) {
                removeData(id, curObj.getKey());
                Trace.info("RM::deleteItem(" + id + ", " + key + ") item deleted" );
                return true;
            }
            else {
                Trace.info("RM::deleteItem(" + id + ", " + key + ") item can't be deleted because some customers reserved it" );
                return false;
            }
        } // if
    }
    

    // query the number of available seats/rooms/cars
    protected int queryNum(int id, String key) {
        Trace.info("RM::queryNum(" + id + ", " + key + ") called" );
        ReservableItem curObj = (ReservableItem) readData( id, key);
        int value = 0;  
        if ( curObj != null ) {
            value = curObj.getCount();
        } // else
        Trace.info("RM::queryNum(" + id + ", " + key + ") returns count = " + value);
        return value;
    }
    
    // query the price of an item
    protected int queryPrice(int id, String key) {
        Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") called" );
        ReservableItem curObj = (ReservableItem) readData( id, key);
        int value = 0; 
        if ( curObj != null ) {
            value = curObj.getPrice();
        } // else
        Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") returns cost = $" + value );
        return value;        
    }
    
    // reserve an item
    protected boolean reserveItem(int id, int customerID, String key, String location) {
        Trace.info("RM::reserveItem( " + id + ", customer=" + customerID + ", " +key+ ", "+location+" ) called" );        
        // Read customer object if it exists (and read lock it)
        // Customer cust = (Customer) readData( id, Customer.getKey(customerID) );        
        // if ( cust == null ) {
        //     Trace.warn("RM::reserveCar( " + id + ", " + customerID + ", " + key + ", "+location+")  failed--customer doesn't exist" );
        //     return false;
        // } 
        
        // check if the item is available
        ReservableItem item = (ReservableItem)readData(id, key);
        if ( item == null ) {
            Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", " + key+", " +location+") failed--item doesn't exist" );
            return false;
        } else if (item.getCount()==0) {
            Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", " + key+", " + location+") failed--No more items" );
            return false;
        } else {            
            // cust.reserve( key, location, item.getPrice());      
            // writeData( id, cust.getKey(), cust );
            
            // decrease the number of available items in the storage
            item.setCount(item.getCount() - 1);
            item.setReserved(item.getReserved()+1);
            
            Trace.info("RM::reserveItem( " + id + ", " + customerID + ", " + key + ", " +location+") succeeded" );
            return true;
        }        
    }
    
    // Create a new flight, or add seats to existing flight
    //  NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
        throws RemoteException
    {
        int old_price = queryFlightPrice(id, flightNum);
        Trace.info("RM::addFlight(" + id + ", " + flightNum + ", $" + flightPrice + ", " + flightSeats + ") called" );
        Flight curObj = (Flight) readData( id, Flight.getKey(flightNum) );
        if ( curObj == null ) {
            // doesn't exist...add it
            Flight newObj = new Flight( flightNum, flightSeats, flightPrice );
            writeData( id, newObj.getKey(), newObj );
            Trace.info("RM::addFlight(" + id + ") created new flight " + flightNum + ", seats = " +
                    flightSeats + ", price = $" + flightPrice );
        } else {
            // add seats to existing flight and update the price...
            curObj.setCount( curObj.getCount() + flightSeats );
            if ( flightPrice > 0 ) {
                curObj.setPrice( flightPrice );
            } // if
            writeData( id, curObj.getKey(), curObj );
            Trace.info("RM::addFlight(" + id + ") modified existing flight " + flightNum + ", seats = " + curObj.getCount() + ", price = $" + flightPrice );
        } // else
        String command = "newflight";
        this.active_txn.get(id).addHistory(command, Integer.toString(flightNum), Integer.toString(flightSeats), Integer.toString(old_price), Integer.toString(flightPrice));
        return(true);
    }

    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice, boolean no_history)
        throws RemoteException
    {
        Trace.info("RM::addFlight(" + id + ", " + flightNum + ", $" + flightPrice + ", " + flightSeats + ") called" );
        Flight curObj = (Flight) readData( id, Flight.getKey(flightNum) );
        if ( curObj == null ) {
            // doesn't exist...add it
            Flight newObj = new Flight( flightNum, flightSeats, flightPrice );
            writeData( id, newObj.getKey(), newObj );
            Trace.info("RM::addFlight(" + id + ") created new flight " + flightNum + ", seats = " +
                    flightSeats + ", price = $" + flightPrice );
        } else {
            // add seats to existing flight and update the price...
            curObj.setCount( curObj.getCount() + flightSeats );
            if ( flightPrice > 0 ) {
                curObj.setPrice( flightPrice );
            } // if
            writeData( id, curObj.getKey(), curObj );
            Trace.info("RM::addFlight(" + id + ") modified existing flight " + flightNum + ", seats = " + curObj.getCount() + ", price = $" + flightPrice );
        } // else
        return(true);
    }


    public boolean deleteFlight(int id, int flightNum)
        throws RemoteException
    {
        int old_price = queryFlightPrice(id, flightNum);
        int old_count = queryFlight(id, flightNum);
        if (deleteItem(id, Flight.getKey(flightNum)))
        {
            String command = "deleteflight";
            this.active_txn.get(id).addHistory(command, Integer.toString(flightNum), Integer.toString(old_count), Integer.toString(old_price));
            return true;
        }
        else return false;
    }



    // Create a new room location or add rooms to an existing location
    //  NOTE: if price <= 0 and the room location already exists, it maintains its current price
    public boolean addRooms(int id, String location, int count, int price)
        throws RemoteException
    {
        int old_price = queryRoomsPrice(id, location);
        Trace.info("RM::addRooms(" + id + ", " + location + ", " + count + ", $" + price + ") called" );
        Hotel curObj = (Hotel) readData( id, Hotel.getKey(location) );
        if ( curObj == null ) {
            // doesn't exist...add it
            Hotel newObj = new Hotel( location, count, price );
            writeData( id, newObj.getKey(), newObj );
            Trace.info("RM::addRooms(" + id + ") created new room location " + location + ", count = " + count + ", price = $" + price );
        } else {
            // add count to existing object and update price...
            curObj.setCount( curObj.getCount() + count );
            if ( price > 0 ) {
                curObj.setPrice( price );
            } // if
            writeData( id, curObj.getKey(), curObj );
            Trace.info("RM::addRooms(" + id + ") modified existing location " + location + ", count = " + curObj.getCount() + ", price = $" + price );
        } // else
        String command = "newroom";
        this.active_txn.get(id).addHistory(command, location, Integer.toString(count), Integer.toString(old_price), Integer.toString(price));
        return(true);
    }

    public boolean addRooms(int id, String location, int count, int price, boolean no_history)
        throws RemoteException
    {
        Trace.info("RM::addRooms(" + id + ", " + location + ", " + count + ", $" + price + ") called" );
        Hotel curObj = (Hotel) readData( id, Hotel.getKey(location) );
        if ( curObj == null ) {
            // doesn't exist...add it
            Hotel newObj = new Hotel( location, count, price );
            writeData( id, newObj.getKey(), newObj );
            Trace.info("RM::addRooms(" + id + ") created new room location " + location + ", count = " + count + ", price = $" + price );
        } else {
            // add count to existing object and update price...
            curObj.setCount( curObj.getCount() + count );
            if ( price > 0 ) {
                curObj.setPrice( price );
            } // if
            writeData( id, curObj.getKey(), curObj );
            Trace.info("RM::addRooms(" + id + ") modified existing location " + location + ", count = " + curObj.getCount() + ", price = $" + price );
        } // else        String command = "newroom";
        return(true);
    }   

    // Delete rooms from a location
    public boolean deleteRooms(int id, String location)
        throws RemoteException
    {
        int old_price = queryRoomsPrice(id, location);
        int old_count = queryRooms(id, location);
        if (deleteItem(id, Hotel.getKey(location)))
        {
            String command = "deleteroom";
            this.active_txn.get(id).addHistory(command, location, Integer.toString(old_count), Integer.toString(old_price));
            return true;
        }
        else return false;
    }

    // Create a new car location or add cars to an existing location
    //  NOTE: if price <= 0 and the location already exists, it maintains its current price
    public boolean addCars(int id, String location, int count, int price)
        throws RemoteException
    {
        int old_price = queryCarsPrice(id, location);
        Trace.info("RM::addCars(" + id + ", " + location + ", " + count + ", $" + price + ") called" );
        Car curObj = (Car) readData( id, Car.getKey(location) );
        if ( curObj == null ) {
            // car location doesn't exist...add it
            Car newObj = new Car( location, count, price );
            writeData( id, newObj.getKey(), newObj );
            Trace.info("RM::addCars(" + id + ") created new location " + location + ", count = " + count + ", price = $" + price );
        } else {
            // add count to existing car location and update price...
            curObj.setCount( curObj.getCount() + count );
            if ( price > 0 ) {
                curObj.setPrice( price );
            } // if
            writeData( id, curObj.getKey(), curObj );
            Trace.info("RM::addCars(" + id + ") modified existing location " + location + ", count = " + curObj.getCount() + ", price = $" + price );
        } // else
        String command = "newcar";
        this.active_txn.get(id).addHistory(command, location, Integer.toString(count), Integer.toString(old_price), Integer.toString(price));
        return(true);
    }

    public boolean addCars(int id, String location, int count, int price, boolean no_history)
        throws RemoteException
    {
        Trace.info("RM::addCars(" + id + ", " + location + ", " + count + ", $" + price + ") called" );
        Car curObj = (Car) readData( id, Car.getKey(location) );
        if ( curObj == null ) {
            // car location doesn't exist...add it
            Car newObj = new Car( location, count, price );
            writeData( id, newObj.getKey(), newObj );
            Trace.info("RM::addCars(" + id + ") created new location " + location + ", count = " + count + ", price = $" + price );
        } else {
            // add count to existing car location and update price...
            curObj.setCount( curObj.getCount() + count );
            if ( price > 0 ) {
                curObj.setPrice( price );
            } // if
            writeData( id, curObj.getKey(), curObj );
            Trace.info("RM::addCars(" + id + ") modified existing location " + location + ", count = " + curObj.getCount() + ", price = $" + price );
        } // else
        return(true);
    }


    // Delete cars from a location
    public boolean deleteCars(int id, String location)
        throws RemoteException
    {
        int old_price = queryCarsPrice(id, location);
        int old_count = queryCars(id, location);
        if (deleteItem(id, Car.getKey(location)))
        {
            String command = "deletecar";
            this.active_txn.get(id).addHistory(command, location, Integer.toString(old_count), Integer.toString(old_price));
            return true;
        }
        else return false;
    }



    // Returns the number of empty seats on this flight
    public int queryFlight(int id, int flightNum)
        throws RemoteException
    {
        return queryNum(id, Flight.getKey(flightNum));
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
        return queryPrice(id, Flight.getKey(flightNum));
    }


    // Returns the number of rooms available at a location
    public int queryRooms(int id, String location)
        throws RemoteException
    {
        return queryNum(id, Hotel.getKey(location));
    }


    
    
    // Returns room price at this location
    public int queryRoomsPrice(int id, String location)
        throws RemoteException
    {
        return queryPrice(id, Hotel.getKey(location));
    }


    // Returns the number of cars available at a location
    public int queryCars(int id, String location)
        throws RemoteException
    {
        return queryNum(id, Car.getKey(location));
    }


    // Returns price of cars at this location
    public int queryCarsPrice(int id, String location)
        throws RemoteException
    {
        return queryPrice(id, Car.getKey(location));
    }

    // Returns data structure containing customer reservation info. Returns null if the
    //  customer doesn't exist. Returns empty RMHashtable if customer exists but has no
    //  reservations.
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
        if (reserveItem(id, customerID, Car.getKey(location), location))
        {
            String s = "car-" + location;
            String key = s.toLowerCase();
            String command = "reservecar";
            this.active_txn.get(id).addHistory(command, Integer.toString(customerID), key, location);
            return true;
        }
        else return false;
    }


    // Adds room reservation to this customer. 
    public boolean reserveRoom(int id, int customerID, String location)
        throws RemoteException
    {
        if (reserveItem(id, customerID, Hotel.getKey(location), location))
        {
            String s = "room-" + location;
            String key = s.toLowerCase();
            String command = "reserveroom";
            this.active_txn.get(id).addHistory(command, Integer.toString(customerID), key, location);
            return true;
        }
        else return false;
    }
    // Adds flight reservation to this customer.  
    public boolean reserveFlight(int id, int customerID, int flightNum)
        throws RemoteException
    {
        if (reserveItem(id, customerID, Flight.getKey(flightNum), String.valueOf(flightNum)))
        {
            String s = "flight-" + flightNum;
            String key = s.toLowerCase();
            String command = "reserveflight";
            this.active_txn.get(id).addHistory(command, Integer.toString(customerID), key, Integer.toString(flightNum));
            return true;
        }
        else return false;
    }
    
    // Reserve an itinerary 
    public boolean itinerary(int id,int customer,Vector flightNumbers,String location,boolean Car,boolean Room)
        throws RemoteException
    {
        return false;
    }

    public void freeItemRes(int id, int customerID,String reservedkey, int reservedCount)
        throws RemoteException
    {
        String command = "freeitemres";
        this.active_txn.get(id).addHistory(command, Integer.toString(customerID),reservedkey, Integer.toString(reservedCount));
        Trace.info("RM::freeItemRes(" + id + ", " + customerID + ") has reserved " + reservedkey + " " +  reservedCount +  " times"  );
        ReservableItem item  = (ReservableItem) readData(id, reservedkey);
        item.setReserved(item.getReserved()-reservedCount);
        item.setCount(item.getCount()+reservedCount);
        // writeData(id, reservedkey, item);
        Trace.info("RM::freeItemRes(" + id + ", " + customerID + ") has reserved " + reservedkey + " which is reserved" +  item.getReserved() +  " times and is still available " + item.getCount() + " times"  );
    }

    public void freeItemRes(int id, int customerID,String reservedkey, int reservedCount, boolean no_history)
        throws RemoteException
    {
        Trace.info("RM::freeItemRes(" + id + ", " + customerID + ") has reserved " + reservedkey + " " +  reservedCount +  " times"  );
        ReservableItem item  = (ReservableItem) readData(id, reservedkey);
        item.setReserved(item.getReserved()-reservedCount);
        item.setCount(item.getCount()+reservedCount);
        // writeData(id, reservedkey, item);
        Trace.info("RM::freeItemRes(" + id + ", " + customerID + ") has reserved " + reservedkey + " which is reserved" +  item.getReserved() +  " times and is still available " + item.getCount() + " times"  );
    }

    public void undoFreeItemRes(int id, int customerID,String reservedkey, int reservedCount)
        throws RemoteException
    {
        Trace.info("RM::undoFreeItemRes(" + id + ", " + customerID + ") has reserved " + reservedkey + " " +  reservedCount +  " times"  );
        ReservableItem item  = (ReservableItem) readData(id, reservedkey);
        item.setReserved(item.getReserved()+reservedCount);
        item.setCount(item.getCount()-reservedCount);
        // writeData(id, reservedkey, item);
        Trace.info("RM::undoFreeItemRes(" + id + ", " + customerID + ") has reserved " + reservedkey + " which is reserved" +  item.getReserved() +  " times and is still available " + item.getCount() + " times"  );
    }


    /* RM do not handle start commit and abort */
    public int start(int transactionId) throws RemoteException
    {
        synchronized(this.active_txn) {
            Transaction txn = new Transaction(transactionId);
            this.active_txn.put(transactionId, txn);
        }
        LogFile log = new LogFile(transactionId);
        this.active_log.put(transactionId, log);
        IOTools.saveToDisk(this.active_log.get(transactionId), rm_name + "_" + Integer.toString(transactionId) + ".log");        
        Trace.info("RM::Transaction " + transactionId + " started to log");
        return transactionId;
    }

    public int prepare(int transactionId) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        Trace.info("RM::Preparing " + transactionId);
        try {java.lang.Thread.sleep(100);}
        catch(Exception e) {}
        String record = "BEFORE_YES";
        this.active_log.get(transactionId).record.add(record);
        IOTools.saveToDisk(this.active_log.get(transactionId), rm_name + "_" + Integer.toString(transactionId) + ".log");
        Trace.info("RM::Transaction " + transactionId + " log updated with BEFORE_YES and saved to disk");
        if (crash_mode == 1) return (selfDestruct(crash_mode)) ? 1 : 0;
        IOTools.saveToDisk(active_txn, ws_fname + Integer.toString(transactionId) + ".txt");
        Trace.info("RM::Transaction " + transactionId + " working set saved to disk");
        try {java.lang.Thread.sleep(100);}
        catch(Exception e) {}
        record = "AFTER_YES";
        this.active_log.get(transactionId).record.add(record);
        IOTools.saveToDisk(this.active_log.get(transactionId), rm_name + "_" + Integer.toString(transactionId) + ".log");      
        Trace.info("RM::Transaction " + transactionId + " log updated with AFTER_YES and saved to disk");
        return 1;
    }

    public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        Trace.info("RM::commit(" + transactionId + ") called");
        synchronized(this.active_txn) {
            if (transactionId < 1 || !this.active_txn.containsKey(transactionId)) {
                Trace.warn("RM::Commit failed--Invalid transactionId");
                throw new InvalidTransactionException(transactionId);
            }
            else
            {
                Trace.info("RM::Committing transaction : " + transactionId);
                try {java.lang.Thread.sleep(100);}
                catch(Exception e) {}
                String record = "BEFORE_COMMIT";
                this.active_log.get(transactionId).record.add(record);
                IOTools.saveToDisk(this.active_log.get(transactionId), rm_name + "_" + Integer.toString(transactionId) + ".log");
                Trace.info("RM::Transaction " + transactionId + " log updated with BEFORE_COMMIT and saved to disk");
                if (crash_mode == 2) return selfDestruct(crash_mode);
                IOTools.saveToDisk(m_itemHT, shadow_fname + Integer.toString(master.getWorkingIndex()) + ".txt");
                Trace.info("RM::data saved to disk");
                master.setLastXid(transactionId);
                master.swap();
                IOTools.saveToDisk(master, mr_fname);
                Trace.info("RM::Master Record saved to disk");

                try {java.lang.Thread.sleep(100);}
                catch(Exception e) {}
                record = "AFTER_COMMIT";
                this.active_log.get(transactionId).record.add(record);
                IOTools.saveToDisk(this.active_log.get(transactionId), rm_name + "_" + Integer.toString(transactionId) + ".log");
                Trace.info("RM::Transaction " + transactionId + " log updated with AFTER_COMMIT and saved to disk");
                IOTools.deleteFile(ws_fname + Integer.toString(transactionId) + ".txt");
                Trace.info("RM::Transaction " + transactionId + " working set deleted from disk");
                this.active_txn.remove(transactionId);
                IOTools.deleteFile(rm_name + "_" + Integer.toString(transactionId) + ".log");
                Trace.info("RM::Transaction " + transactionId + " log deleted from disk");
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
        Trace.info("RM::Aborting transaction : " + transactionId);
        Stack<Vector> history = getHistory(transactionId);
        if (history == null)
        {
            IOTools.deleteFile(ws_fname + Integer.toString(transactionId) + ".txt");
            this.active_txn.remove(transactionId);
            Trace.info("RM::Transaction " + transactionId + " working set deleted from disk");
            IOTools.deleteFile(rm_name + "_" + Integer.toString(transactionId) + ".log");
            this.active_log.remove(transactionId);
            Trace.info("RM::Transaction " + transactionId + " log deleted from disk");
            return;
        }
        try {java.lang.Thread.sleep(100);}
        catch(Exception e) {}
        String record = "BEFORE_ABORT";
        this.active_log.get(transactionId).record.add(record);
        IOTools.saveToDisk(this.active_log.get(transactionId), rm_name + "_" + Integer.toString(transactionId) + ".log");
        Trace.info("RM::Transaction " + transactionId + " log updated with BEFORE_ABORT and saved to disk");
        if (crash_mode == 2) 
        {
            selfDestruct(crash_mode);
            return;
        }
        while (!history.empty())
        {
            Vector<String> v = history.pop();
            // System.out.println(v.get(0));
            if (v.get(0).equals("newcar"))
            {
                removeCars(transactionId, v.get(1), v.get(2), v.get(3));
            }
            else if (v.get(0).equals("deletecar"))
            {
                recoverCars(transactionId, v.get(1), v.get(2), v.get(3));
            }
            else if (v.get(0).equals("reservecar"))
            {
                int customer = Integer.parseInt(v.get(1));
                freeItemRes(transactionId, customer, v.get(2), 1, true);
            }
            else if (v.get(0).equals("newroom"))
            {
                removeRooms(transactionId, v.get(1), v.get(2), v.get(3));
            }
            else if (v.get(0).equals("deleteroom"))
            {
                recoverRooms(transactionId, v.get(1), v.get(2), v.get(3));
            }
            else if (v.get(0).equals("reserveroom"))
            {
                int customer = Integer.parseInt(v.get(1));
                freeItemRes(transactionId, customer, v.get(2), 1, true);
            }
            else if (v.get(0).equals("newflight"))
            {
                removeFlight(transactionId, v.get(1), v.get(2), v.get(3));
            }
            else if (v.get(0).equals("deleteflight"))
            {
                recoverFlight(transactionId, v.get(1), v.get(2), v.get(3));
            }
            else if (v.get(0).equals("reserveflight"))
            {
                int customer = Integer.parseInt(v.get(1));
                freeItemRes(transactionId, customer, v.get(2), 1, true);
            }
            else if (v.get(0).equals("freeitemres"))
            {
                int customerID = Integer.parseInt(v.get(1));
                String reservedkey = v.get(2);
                int reservedCount = Integer.parseInt(v.get(3));
                undoFreeItemRes(transactionId, customerID, reservedkey, reservedCount);
            }
        }
        try {java.lang.Thread.sleep(100);}
        catch(Exception e) {}
        record = "AFTER_ABORT";
        this.active_log.get(transactionId).record.add(record);
        IOTools.saveToDisk(this.active_log.get(transactionId), rm_name + "_" + Integer.toString(transactionId) + ".log");
        Trace.info("RM::Transaction " + transactionId + " log updated with AFTER_ABORT and saved to disk");
        IOTools.deleteFile(ws_fname + Integer.toString(transactionId) + ".txt");
        this.active_txn.remove(transactionId);
        Trace.info("RM::Transaction " + transactionId + " working set deleted from disk");
        IOTools.deleteFile(rm_name + "_" + Integer.toString(transactionId) + ".log");
        this.active_log.remove(transactionId);
        Trace.info("RM::Transaction " + transactionId + " log deleted from disk");
        if (crash_mode == 3) 
        {
            selfDestruct(crash_mode);
            return;
        }
    }


    public boolean shutdown() throws RemoteException
    {
        /* TODO: store data? */
        Registry registry = LocateRegistry.getRegistry(2199);
        try {
          registry.unbind(rm_name);
          UnicastRemoteObject.unexportObject(this, false);
        } catch (Exception e) {
          throw new RemoteException("Could not unregister service, quiting anyway", e);
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

    public void pop(int id)
    {
        this.active_txn.get(id).txn_hist.pop();
    }

    public boolean removeFlight(int id, String sflightNum, String sflightSeats, String sold_flightPrice)
    throws RemoteException
    {
        int flightNum = Integer.parseInt(sflightNum);
        int flightSeats = Integer.parseInt(sflightSeats);
        int old_flightPrice = Integer.parseInt(sold_flightPrice);
        Trace.info("RM::removeFlight(" + id + ", " + flightNum + ", " + flightSeats + ", " + old_flightPrice + ") called" );
        Flight curObj = (Flight) readData( id, Flight.getKey(flightNum));
        // Check if there is such an item in the storage
        if ( curObj == null ) {
            Trace.warn("RM::removeFlight(" + id + ", " + flightNum + ", " + flightSeats + ", " + old_flightPrice + ") failed--item doesn't exist" );
            return false;
        } else {
            if (curObj.getCount() < flightSeats) {
                Trace.info("RM::removeFlight(" + id + ", " + flightNum + ", " + flightSeats + ", " + old_flightPrice + ") failed-- insufficient count" );
                return false;
            }
            else {
                curObj.setCount(curObj.getCount() - flightSeats);
                curObj.setPrice(old_flightPrice);
                writeData( id, curObj.getKey(), curObj );
                Trace.info("RM::removeFlight(" + id + ", " + flightNum + ", " + flightSeats + ", " + old_flightPrice + ") item removed" );
                return true;
            }
        } // if
    }

    public boolean removeRooms(int id, String slocation, String scount, String sold_price)
    throws RemoteException
    {
        String location = slocation;
        int count = Integer.parseInt(scount);
        int old_price = Integer.parseInt(sold_price);
        Trace.info("RM::removeRooms(" + id + ", " + location + ", " + count + ", " + old_price + ") called" );
        Hotel curObj = (Hotel) readData( id, Hotel.getKey(location));
        // Check if there is such an item in the storage
        if ( curObj == null ) {
            Trace.warn("RM::removeRooms(" + id + ", " + location + ", " + count + ", " + old_price + ") failed--item doesn't exist" );
            return false;
        } else {
            if (curObj.getCount() < count) {
                Trace.info("RM::removeRooms(" + id + ", " + location + ", " + count + ", " + old_price + ") failed-- insufficient count" );
                return false;
            }
            else {
                curObj.setCount(curObj.getCount() - count);
                curObj.setPrice(old_price);
                writeData( id, curObj.getKey(), curObj );
                Trace.info("RM::removeRooms(" + id + ", " + location + ", " + count + ", " + old_price + ") item removed" );
                return true;
            }
        } // if
    }

    public boolean removeCars(int id, String slocation, String scount, String sold_price)
    throws RemoteException
    {
        String location = slocation;
        int count = Integer.parseInt(scount);
        int old_price = Integer.parseInt(sold_price);
        Trace.info("RM::removeCars(" + id + ", " + location + ", " + count + ", " + old_price + ") called" );
        Car curObj = (Car) readData( id, Car.getKey(location));
        // Check if there is such an item in the storage
        if ( curObj == null ) {
            Trace.warn("RM::removeCars(" + id + ", " + location + ", " + count + ", " + old_price + ") failed--item doesn't exist" );
            return false;
        } else {
            if (curObj.getCount() < count) {
                Trace.info("RM::removeCars(" + id + ", " + location + ", " + count + ", " + old_price + ") failed-- insufficient count" );
                return false;
            }
            else {
                curObj.setCount(curObj.getCount() - count);
                curObj.setPrice(old_price);
                writeData( id, curObj.getKey(), curObj );
                Trace.info("RM::removeRooms(" + id + ", " + location + ", " + count + ", " + old_price + ") item removed" );
                return true;
            }
        } // if
    }

    public boolean recoverFlight(int id, String sflightNum, String sflightSeats, String sold_flightPrice)
    throws RemoteException
    {
        Trace.info("RM::recoverFlight(" + id + ", " + sflightNum + ", " + sflightSeats + ", " + sold_flightPrice + ") called");
        int flightNum = Integer.parseInt(sflightNum);
        int flightSeats = Integer.parseInt(sflightSeats);
        int old_flightPrice = Integer.parseInt(sold_flightPrice);
        return addFlight(id, flightNum, flightSeats, old_flightPrice, true);
    }

    public boolean recoverRooms(int id, String slocation, String scount, String sold_price)
    throws RemoteException
    {
        Trace.info("RM::recoverRooms(" + id + ", " + slocation + ", " + scount + ", " + sold_price + ") called");
        String location = slocation;
        int count = Integer.parseInt(scount);
        int old_price = Integer.parseInt(sold_price);
        return addRooms(id, location, count, old_price, true);
    }

    public boolean recoverCars(int id, String slocation, String scount, String sold_price)
    throws RemoteException
    {
        Trace.info("RM::recoverCars(" + id + ", " + slocation + ", " + scount + ", " + sold_price + ") called");
        String location = slocation;
        int count = Integer.parseInt(scount);
        int old_price = Integer.parseInt(sold_price);
        return addCars(id, location, count, old_price, true);
    }

    public void setCrashMode(int mode)
    {
        Trace.info("RM::setCrashMode(" + mode + ") called");
        crash_mode = mode;
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

    public int getTransactionId(String filename) {
        // return filename.charAt(filename.length()-5) - '0';
        String[] tmp = filename.replace('.','_').split("_");
        // for (String s : tmp) System.out.println("zz : " + s);
        return Integer.parseInt(tmp[1]);
    }
    
    public void recoverResourceManagerStatus() throws InvalidTransactionException{
        Trace.info("RM::Recovering server data");
        File folder = new File(".");
        for (File f: folder.listFiles()) {
            try {
                String filename = f.getName();
                if (null != filename) {
                    if (this.rm_name.equals("CarRM") && filename.startsWith("Car") && filename.endsWith(".log")) {
                        int transactionId = getTransactionId(filename);
                        File file = new File("CarRM_WS_" + transactionId + ".txt");
                        if (file.exists()) {
                            this.active_txn = (Hashtable) IOTools.loadFromDisk("CarRM_WS_" + transactionId + ".txt");
                            Trace.info("RM::Transaction " + transactionId + " working set loaded from disk");
                        } else this.active_txn = new Hashtable<Integer, Transaction>();
                        LogFile log = (LogFile) IOTools.loadFromDisk(filename);
                        this.active_log.put(transactionId, log);
                        Trace.info("RM::Transaction " + transactionId + " log loaded from disk");
                        if (log.record.contains("BEFORE_ABORT") || log.record.contains("AFTER_ABORT")) {
                            IOTools.deleteFile("CarRM_WS_" + Integer.toString(transactionId) + ".txt");
                            Trace.info("RM::Transaction " + transactionId + " working set deleted from disk");
                            IOTools.deleteFile("CarRM" + "_" + Integer.toString(transactionId) + ".log");
                            Trace.info("RM::Transaction " + transactionId + " log deleted from disk");
                            this.active_log.remove(transactionId);
                            this.active_txn.remove(transactionId);                            
                            continue;
                        }

                        if (transactionId < 1 || (!this.active_txn.containsKey(transactionId)&&this.active_txn.size()!=0)) {
                            throw new InvalidTransactionException(transactionId);
                        }
                        else
                        {
                            if (log.record.size() == 4) {
                                //Do nothing   
                            } else if (log.record.size() == 3 || log.record.size() == 2) {
                                if (mw.get_votes_result(transactionId) == true) {
                                    this.recover_history(transactionId);
                                } else this.commit_no_crash(transactionId);
                                Trace.info("RM::Transaction " + transactionId + " history recovered from disk");
                            } else if (log.record.size() == 1 || log.record.size() == 0) {
                                // IOTools.deleteFile("CarRM_WS_" + Integer.toString(transactionId) + ".txt");
                                // Trace.info("RM::Transaction " + transactionId + " working set deleted from disk");
                                // IOTools.deleteFile("CarRM" + "_" + Integer.toString(transactionId) + ".log");
                                // Trace.info("RM::Transaction " + transactionId + " log deleted from disk");
                                // this.active_log.remove(transactionId);
                                this.active_txn.remove(transactionId);
                                mw.abort(transactionId);
                                Trace.info("RM::Transaction " + transactionId + " removed from active transactions in Transaction Manager");
                            }
                        }
                    }
                    if (this.rm_name.equals("FlightRM") && filename.startsWith("Flight") && filename.endsWith(".log")) {
                        int transactionId = getTransactionId(filename);
                        File file = new File("FlightRM_WS_" + transactionId + ".txt");
                        if (file.exists()) {
                            this.active_txn = (Hashtable) IOTools.loadFromDisk("FlightRM_WS_" + transactionId + ".txt");
                            Trace.info("RM::Transaction " + transactionId + " working set loaded from disk");
                        } else this.active_txn = new Hashtable<Integer, Transaction>();
                        LogFile log = (LogFile) IOTools.loadFromDisk(filename);
                        this.active_log.put(transactionId, log);
                        Trace.info("RM::Transaction " + transactionId + " log loaded from disk");
                        if (log.record.contains("BEFORE_ABORT") || log.record.contains("AFTER_ABORT")) {
                            IOTools.deleteFile("FlightRM_WS_" + Integer.toString(transactionId) + ".txt");
                            Trace.info("RM::Transaction " + transactionId + " working set deleted from disk");
                            IOTools.deleteFile("FlightRM" + "_" + Integer.toString(transactionId) + ".log");
                            Trace.info("RM::Transaction " + transactionId + " log deleted from disk");
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
                                if (mw.get_votes_result(transactionId) == true) {
                                    this.recover_history(transactionId);
                                } else this.commit_no_crash(transactionId);
                                Trace.info("RM::Transaction " + transactionId + " history recovered from disk");
                            } else if (log.record.size() == 1 || log.record.size() == 0) {
                                // IOTools.deleteFile("FlightRM_WS_" + Integer.toString(transactionId) + ".txt");
                                // Trace.info("RM::Transaction " + transactionId + " working set deleted from disk");
                                // IOTools.deleteFile("FlightRM" + "_" + Integer.toString(transactionId) + ".log");
                                // Trace.info("RM::Transaction " + transactionId + " log deleted from disk");
                                // this.active_log.remove(transactionId);
                                this.active_txn.remove(transactionId);
                                mw.abort(transactionId);
                                Trace.info("RM::Transaction " + transactionId + " removed from active transactions in Transaction Manager");
                            }
                        }
                    }
                    if (this.rm_name.equals("RoomRM") && filename.startsWith("Room") && filename.endsWith(".log")) {
                        int transactionId = getTransactionId(filename);
                        File file = new File("RoomRM_WS_" + transactionId + ".txt");
                        if (file.exists()) {
                            this.active_txn = (Hashtable) IOTools.loadFromDisk("RoomRM_WS_" + transactionId + ".txt");
                            Trace.info("RM::Transaction " + transactionId + " working set loaded from disk");
                        } else this.active_txn = new Hashtable<Integer, Transaction>();
                        LogFile log = (LogFile) IOTools.loadFromDisk(filename);
                        this.active_log.put(transactionId, log);
                        Trace.info("RM::Transaction " + transactionId + " log loaded from disk");
                        if (log.record.contains("BEFORE_ABORT") || log.record.contains("AFTER_ABORT")) {
                            IOTools.deleteFile("RoomRM_WS_" + Integer.toString(transactionId) + ".txt");
                            Trace.info("RM::Transaction " + transactionId + " working set deleted from disk");
                            IOTools.deleteFile("RoomRM" + "_" + Integer.toString(transactionId) + ".log");
                            Trace.info("RM::Transaction " + transactionId + " log deleted from disk");
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
                                if (mw.get_votes_result(transactionId) == true) {
                                    this.recover_history(transactionId);
                                } else this.commit_no_crash(transactionId);
                                Trace.info("RM::Transaction " + transactionId + " history recovered from disk");
                            } else if (log.record.size() == 1 || log.record.size() == 0) {
                                // IOTools.deleteFile("RoomRM_WS_" + Integer.toString(transactionId) + ".txt");
                                // Trace.info("RM::Transaction " + transactionId + " working set deleted from disk");
                                // IOTools.deleteFile("RoomRM" + "_" + Integer.toString(transactionId) + ".log");
                                // Trace.info("RM::Transaction " + transactionId + " log deleted from disk");
                                // this.active_log.remove(transactionId);
                                this.active_txn.remove(transactionId);
                                mw.abort(transactionId);
                                Trace.info("RM::Transaction " + transactionId + " removed from active transactions in Transaction Manager");
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                System.out.println(e.getMessage()); 
            }
        }
    }

    public boolean recover_history(int transactionId) throws RemoteException
    {
        Trace.info("RM::Recovering Transaction " + transactionId + " history");
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
            // System.out.println(v.get(0));
            if (v.get(0).equals("newcar"))
            {
                // removeCars(transactionId, v.get(1), v.get(2), v.get(3));
                addCars(transactionId, v.get(1), Integer.parseInt(v.get(2)), Integer.parseInt(v.get(4)), true);
            }
            else if (v.get(0).equals("deletecar"))
            {
                // recoverCars(transactionId, v.get(1), v.get(2), v.get(3));
                deleteCars(transactionId, v.get(1));
            }
            else if (v.get(0).equals("reservecar"))
            {
                int customer = Integer.parseInt(v.get(1));
                // freeItemRes(transactionId, customer, v.get(2), 1, true);
                reserveCar(transactionId, customer, v.get(2));
            }
            else if (v.get(0).equals("newroom"))
            {
                // removeRooms(transactionId, v.get(1), v.get(2), v.get(3));
                addRooms(transactionId, v.get(1), Integer.parseInt(v.get(2)), Integer.parseInt(v.get(4)), true);
            }
            else if (v.get(0).equals("deleteroom"))
            {
                // recoverRooms(transactionId, v.get(1), v.get(2), v.get(3));
                deleteRooms(transactionId, v.get(1));
            }
            else if (v.get(0).equals("reserveroom"))
            {
                int customer = Integer.parseInt(v.get(1));
                // freeItemRes(transactionId, customer, v.get(2), 1, true);
                reserveRoom(transactionId, customer, v.get(2));
            }
            else if (v.get(0).equals("newflight"))
            {
                // removeFlight(transactionId, v.get(1), v.get(2), v.get(3));
                addFlight(transactionId, Integer.parseInt(v.get(1)),  Integer.parseInt(v.get(2)), Integer.parseInt(v.get(4)), true);
            }
            else if (v.get(0).equals("deleteflight"))
            {
                // recoverFlight(transactionId, v.get(1), v.get(2), v.get(3));
                deleteFlight(transactionId, Integer.parseInt(v.get(1)));
            }
            else if (v.get(0).equals("reserveflight"))
            {
                int customer = Integer.parseInt(v.get(1));
                // freeItemRes(transactionId, customer, v.get(2), 1, true);
                reserveFlight(transactionId, customer, Integer.parseInt(v.get(2)));
            }
            else if (v.get(0).equals("freeitemres"))
            {
                int customerID = Integer.parseInt(v.get(1));
                String reservedkey = v.get(2);
                int reservedCount = Integer.parseInt(v.get(3));
                freeItemRes(transactionId, customerID, reservedkey, reservedCount);
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
                Trace.info("RM::Customer data saved to disk");
                master.setLastXid(transactionId);
                master.swap();
                IOTools.saveToDisk(master, mr_fname);
                Trace.info("RM::Master Record saved to disk");
                
                IOTools.deleteFile(ws_fname + Integer.toString(transactionId) + ".txt");
                this.active_txn.remove(transactionId);
                Trace.info("RM::Transaction " + transactionId + " working set deleted to disk");
                IOTools.deleteFile(rm_name + "_" + Integer.toString(transactionId) + ".log");
                this.active_log.remove(transactionId);
                Trace.info("RM::Transaction " + transactionId + " log deleted to disk");
            }
        }
        return true;
    }
}