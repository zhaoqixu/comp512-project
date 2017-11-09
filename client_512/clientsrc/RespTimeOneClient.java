import MidInterface.*;
import MidImpl.TransactionAbortedException;
import MidImpl.InvalidTransactionException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.util.*;
import java.io.*;
import java.io.BufferedWriter;
import java.io.FileWriter;

    
public class RespTimeOneClient
{
    static String message = "blank";
    static MiddleWare mw = null;
    public static void main(String args[])
    {
        RespTimeOneClient obj = new RespTimeOneClient();
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        String command = "";
        Vector arguments  = new Vector();
        int Id, Cid;
        int flightNum;
        int flightPrice;
        int flightSeats;
        boolean Room;
        boolean Car;
        int price;
        int numRooms;
        int numCars;
        String location;
        int type = 1; // 1: one RM, 2: multiple RMs

        String server = "localhost";
        int port = 1088;
        if (args.length > 0)
        {
            server = args[0];
        }
        if (args.length > 1)
        {
            type = Integer.parseInt(args[1]);
        }
        if (args.length > 2)
        {
            System.out.println ("Usage: java client [rmihost [rmiport]]");
            System.exit(1);
        }
        
        try 
        {
            // get a reference to the rmiregistry
            Registry registry = LocateRegistry.getRegistry(server, port);
            // get the proxy and the remote reference by rmiregistry lookup
            mw = (MiddleWare) registry.lookup("TripMiddleWare");
            if(mw!=null)
            {
                System.out.println("Successful");
                System.out.println("Connected to MW");
            }
            else
            {
                System.out.println("Unsuccessful");
            }
            // make call on remote method
        } 
        catch (Exception e) 
        {    
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
        
        if (System.getSecurityManager() == null) {
            //System.setSecurityManager(new RMISecurityManager());
        }

        int iterations = 1000; // the number of transactions submitted during the test run        
        int iterations_warmup = 100;
        warmUp(type, iterations_warmup);
        long before = System.currentTimeMillis();
        for(int i=0; i<iterations; i++){
            // long start = System.currentTimeMillis();
            if (type == 1) {
                oneRM();
            } else {
                multiRM();
            }
            // long end = System.currentTimeMillis();
            // System.out.println(end-start);
        }
        long after = System.currentTimeMillis();
        System.out.println(after-before);
    }
    
    public static void warmUp(int type, int iterations_warmup) {
        try {
            if (type == 1) {
                for(int i=0; i<iterations_warmup; i++) {
                    oneRM();
                }
            } else {
                for(int i=0; i<iterations_warmup; i++) {
                    multiRM();
                }
            }
        }
        catch(Exception e) {
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public static void oneRM() {
        try {
            int transactionId = mw.start();
            txnTypeOne(transactionId);
            mw.commit(transactionId);
        }
        catch(Exception e) {
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public static void multiRM() {
        try {
            int transactionId = mw.start();
            txnTypeTwo(transactionId);
            mw.commit(transactionId);
        }
        catch(Exception e) {
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public static void txnTypeOne(int transactionId) throws Exception{
        mw.addCars(transactionId,"Montreal",3,1000);
        mw.queryCars(transactionId,"Montreal");
        mw.queryCarsPrice(transactionId,"Montreal");
        mw.deleteCars(transactionId,"Montreal");

        mw.addCars(transactionId,"Toronto",3,2000);
        mw.queryCars(transactionId,"Toronto");
        mw.queryCarsPrice(transactionId,"Toronto");
        mw.deleteCars(transactionId,"Toronto");

        mw.addCars(transactionId,"Ottawa",3,3000);
        mw.queryCars(transactionId,"Ottawa");
        mw.queryCarsPrice(transactionId,"Ottawa");
        mw.deleteCars(transactionId,"Ottawa");
    }

    public static void txnTypeTwo(int transactionId) throws Exception{
        mw.addFlight(transactionId,1000,3,2000);
        mw.queryFlight(transactionId,1000);
        mw.queryFlightPrice(transactionId,1000);
        mw.deleteFlight(transactionId,1000);

        mw.addCars(transactionId,"Toronto",3,2000);
        mw.queryCars(transactionId,"Toronto");
        mw.queryCarsPrice(transactionId,"Toronto");
        mw.deleteCars(transactionId,"Toronto");

        mw.addRooms(transactionId,"Ottawa",3,3000);
        mw.queryRooms(transactionId,"Ottawa");
        mw.queryRoomsPrice(transactionId,"Ottawa");
        mw.deleteRooms(transactionId,"Ottawa");
    }

    // public static void txnTypeThree(int transactionId) {
        
    // }
        
}