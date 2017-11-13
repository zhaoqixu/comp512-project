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
        int transaction_type = 1;     
        boolean multiRM = false;        
        String server = "localhost";
        int port = 1088;
        if (args.length > 0)
        {
            server = args[0];
        }
        if (args.length > 1)
        {
            transaction_type = Integer.parseInt(args[1]);
        }
        if (args.length > 2)
        {
            multiRM = Boolean.parseBoolean(args[2]);
        }
        if (args.length > 3)
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
        warmUp(multiRM, iterations_warmup);
        long before = System.currentTimeMillis();
        for(int i=0; i<iterations; i++){
            // long start = System.currentTimeMillis();
            if (multiRM) {
                multiRM(transaction_type);            
            } else {
                oneRM(transaction_type);
            }
            // long end = System.currentTimeMillis();
            // System.out.println(end-start);
        }
        long after = System.currentTimeMillis();
        System.out.println(after-before);
        try {
            mw.shutdown();
        }
        catch (Exception e) {}
    }
    
    public static void warmUp(boolean multiRM, int iterations_warmup) {
        try {
            Random rn = new Random();
            if (!multiRM) {
                for(int i=0; i<iterations_warmup; i++) {
                    int transactionId = mw.start();
                    mw.addCars(transactionId,"Beijing",3,1000);
                    //////////////////////////////////////////
                    mw.addCars(transactionId,"Montreal",3,1000);
                    mw.queryCars(transactionId,"Montreal");
                    mw.queryCarsPrice(transactionId,"Montreal");
                    mw.deleteCars(transactionId,"Montreal");

                    mw.addCars(transactionId,"Montreal",3,1000);
                    mw.queryCars(transactionId,"Montreal");
                    mw.queryCarsPrice(transactionId,"Montreal");
                    mw.deleteCars(transactionId,"Montreal");

                    mw.addCars(transactionId,"Montreal",3,1000);
                    mw.queryCars(transactionId,"Montreal");
                    mw.queryCarsPrice(transactionId,"Montreal");
                    mw.deleteCars(transactionId,"Montreal");

                    mw.commit(transactionId);
                }
            } else {
                for(int i=0; i<iterations_warmup; i++) {
                    int transactionId = mw.start();
                    mw.addCars(transactionId,"Beijing",3,1000);
                    mw.addFlight(transactionId,12345,3,2000);
                    mw.addRooms(transactionId,"Shanghai",3,3000);
                    /////////////////////////////////////////////
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

                    mw.commit(transactionId);
                }
            }
        }
        catch(Exception e) {
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public static void oneRM(int transaction_type) {
        try {
            int transactionId = mw.start();            
            switch(transaction_type) {
                case 1:
                    txnTypeOne(transactionId);
                    mw.commit(transactionId);
                    break;
                case 2:
                    txnTypeOne(transactionId);
                    mw.abort(transactionId);
                    break;
                case 3:
                    txnTypeThree(transactionId);
                    mw.commit(transactionId);
                    break;
                case 4:
                    txnTypeThree(transactionId);
                    mw.abort(transactionId);
                    break;
                case 5:
                    txnTypeFive(transactionId);
                    mw.commit(transactionId);
                    break;
                case 6:
                    txnTypeFive(transactionId);
                    mw.abort(transactionId);
                    break;
            }
        }
        catch(Exception e) {
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public static void multiRM(int transaction_type) {
        try {
            int transactionId = mw.start();         
            switch(transaction_type) {
                case 1:
                    txnTypeTwo(transactionId);
                    mw.commit(transactionId);
                    break;
                case 2:
                    txnTypeTwo(transactionId);
                    mw.abort(transactionId);
                    break;
                case 3:
                    txnTypeFour(transactionId);
                    mw.commit(transactionId);
                    break;
                case 4:
                    txnTypeFour(transactionId);
                    mw.abort(transactionId);
                    break;
                case 5:
                    txnTypeSix(transactionId);
                    mw.commit(transactionId);
                    break;
                case 6:
                    txnTypeSix(transactionId);
                    mw.abort(transactionId);
                    break;
            }
        }
        catch(Exception e) {
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    // 12 read oneRM
    public static void txnTypeOne(int transactionId) throws Exception{
        mw.queryCars(transactionId,"Beijing");
        mw.queryCarsPrice(transactionId,"Beijing");
        mw.queryCars(transactionId,"Beijing");
        mw.queryCarsPrice(transactionId,"Beijing");
        mw.queryCars(transactionId,"Beijing");
        mw.queryCarsPrice(transactionId,"Beijing");
        mw.queryCars(transactionId,"Beijing");
        mw.queryCarsPrice(transactionId,"Beijing");
        mw.queryCars(transactionId,"Beijing");
        mw.queryCarsPrice(transactionId,"Beijing");
        mw.queryCars(transactionId,"Beijing");
        mw.queryCarsPrice(transactionId,"Beijing");
    }

    // 12 read multiRM
    public static void txnTypeTwo(int transactionId) throws Exception{
        mw.queryFlight(transactionId,12345);
        mw.queryFlightPrice(transactionId,12345);
        mw.queryCars(transactionId,"Beijing");
        mw.queryCarsPrice(transactionId,"Beijing");
        mw.queryRooms(transactionId,"Shanghai");
        mw.queryRoomsPrice(transactionId,"Shanghai");
        mw.queryFlight(transactionId,12345);
        mw.queryFlightPrice(transactionId,12345);
        mw.queryCars(transactionId,"Beijing");
        mw.queryCarsPrice(transactionId,"Beijing");
        mw.queryRooms(transactionId,"Shanghai");
        mw.queryRoomsPrice(transactionId,"Shanghai");
    }

    // 12 write oneRM
    public static void txnTypeThree(int transactionId) throws Exception{
        mw.addCars(transactionId,"Montreal",3,1000);
        mw.deleteCars(transactionId,"Montreal");        
        mw.addCars(transactionId,"Montreal",3,1000);        
        mw.deleteCars(transactionId,"Montreal");
        
        mw.addCars(transactionId,"Toronto",3,2000);
        mw.deleteCars(transactionId,"Toronto");        
        mw.addCars(transactionId,"Toronto",3,2000);
        mw.deleteCars(transactionId,"Toronto");

        mw.addCars(transactionId,"Ottawa",3,3000);
        mw.deleteCars(transactionId,"Ottawa");
        mw.addCars(transactionId,"Toronto",3,2000);
        mw.deleteCars(transactionId,"Toronto");
    }

    // 12 write  multiRM
    public static void txnTypeFour(int transactionId) throws Exception{
        mw.addCars(transactionId,"Montreal",3,1000);
        mw.deleteCars(transactionId,"Montreal");
        mw.addCars(transactionId,"Montreal",3,1000);
        mw.deleteCars(transactionId,"Montreal");

        mw.addFlight(transactionId,1000,3,2000);
        mw.deleteFlight(transactionId,1000);
        mw.addFlight(transactionId,1000,3,2000);
        mw.deleteFlight(transactionId,1000);

        mw.addRooms(transactionId,"Ottawa",3,3000);
        mw.deleteRooms(transactionId,"Ottawa");
        mw.addRooms(transactionId,"Ottawa",3,3000);
        mw.deleteRooms(transactionId,"Ottawa");
    }

    // 6 read + 6 write oneRM
    public static void txnTypeFive(int transactionId) throws Exception{
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

    // 6 read + 6 write multiRM
    public static void txnTypeSix(int transactionId) throws Exception{
        mw.addCars(transactionId,"Montreal",3,1000);
        mw.queryCars(transactionId,"Montreal");
        mw.queryCarsPrice(transactionId,"Montreal");
        mw.deleteCars(transactionId,"Montreal");

        mw.addFlight(transactionId,1000,3,2000);
        mw.queryFlight(transactionId,1000);
        mw.queryFlightPrice(transactionId,1000);
        mw.deleteFlight(transactionId,1000);

        mw.addRooms(transactionId,"Ottawa",3,3000);
        mw.queryRooms(transactionId,"Ottawa");
        mw.queryRoomsPrice(transactionId,"Ottawa");
        mw.deleteRooms(transactionId,"Ottawa");
    }
    
}