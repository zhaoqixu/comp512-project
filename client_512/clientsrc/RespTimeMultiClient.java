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
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
    
public class RespTimeMultiClient
{
    static int client_num = 10;
    static int wait = 0;
    static int iterations = 100; // the number of transactions submitted during the test run
    static String message = "blank";
    static MiddleWare mw = null;
    public static void main(String args[])
    {
        RespTimeOneClient obj = new RespTimeOneClient();

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
            client_num = Integer.parseInt(args[2]);
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
 
        int iterations_warmup = 100;
        warmUp(type, iterations_warmup);
        // long before = System.currentTimeMillis();
        // for(int i=0; i<iterations; i++){
        //     long start = System.currentTimeMillis();
        //     if (type == 1) {
        //         oneRM();
        //     } else {
        //         multiRM();
        //     }
        //     long end = System.currentTimeMillis();
        //     System.out.println(end-start);
        // }
        // long after = System.currentTimeMillis();
        // System.out.println(after-before);
        System.out.println("Starting threads!");
        ExecutorService exe = Executors.newFixedThreadPool(client_num);
		try {
		    AutoClient[] clients = new AutoClient[client_num];
		    for (int i = 0; i < client_num; ++i) {
                clients[i] = new AutoClient(i, mw, type);
            }
            exe.invokeAll(Arrays.asList(clients), 60, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            System.out.println("INTERRUPTION IN EXECUTOR!!!");
        }
        // try {
        //     mw.shutdown();
        // }
        // catch (Exception e) {}
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
}

class AutoClient implements Callable<Void>
{
    private MiddleWare mw;
    private int cid;
    private int type;

    public AutoClient(int cid, MiddleWare mw, int type)
    {
        this.cid = cid;
        this.mw = mw;
        this.type = type;
    }

    public Void call()
    {
        int count = 0;
        long time = 0;
        int time_interval = 500;
        for (int i = 0; i < 1000; i++)
        {
            try {
                long t1 = System.nanoTime();
                if (type == 1)
                {
                    oneRM();
                }
                else{
                    multiRM();
                }
                long t2 = System.nanoTime();
                count ++;
                time += (t2-t1)/1000000.0;
                // System.out.printf("%d , %.3f%n", cid, (t2-t1)/1000000.0);
                // try {
                Random rand = new Random();
                long randomNum = (long)rand.nextInt((50) + 1) + time_interval - 25;
                long sleep_time = randomNum - (t2 - t1) / (long)1000000.0;
                if (sleep_time > 0)
                    Thread.sleep(sleep_time);
                // else System.out.println("NO SLEEP!");
                // }
                // catch(InterruptedException ie){}
            }
            catch (Exception e)
            {
                // System.out.println(1);
                // System.out.println("EXCEPTION:");
                // System.out.println(e.getMessage());
                // e.printStackTrace();
            }
        }
        System.out.println("count : " + count + " time : " + time + " avg : " + time / (long) count);
        return null;
    }

    public void oneRM() {
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

    public void multiRM() {
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

    public void txnTypeOne(int transactionId) throws Exception{
        String sm = "Montreal" + Integer.toString(transactionId);
        String st = "Toronto" + Integer.toString(transactionId);
        String so = "Ottawa" + Integer.toString(transactionId);

        mw.addCars(transactionId,sm,3,1000);
        mw.queryCars(transactionId,sm);
        mw.queryCarsPrice(transactionId,sm);
        mw.deleteCars(transactionId,sm);

        mw.addCars(transactionId,st,3,2000);
        mw.queryCars(transactionId,st);
        mw.queryCarsPrice(transactionId,st);
        mw.deleteCars(transactionId,st);

        mw.addCars(transactionId,so,3,3000);
        mw.queryCars(transactionId,so);
        mw.queryCarsPrice(transactionId,so);
        mw.deleteCars(transactionId,so);
    }

    public void txnTypeTwo(int transactionId) throws Exception{
        int flightNum = transactionId * 1000;
        String st = "Toronto" + Integer.toString(transactionId);
        String so = "Ottawa" + Integer.toString(transactionId);

        mw.addFlight(transactionId,flightNum,3,2000);
        mw.queryFlight(transactionId,flightNum);
        mw.queryFlightPrice(transactionId,flightNum);
        mw.deleteFlight(transactionId,flightNum);

        mw.addCars(transactionId,st,3,2000);
        mw.queryCars(transactionId,st);
        mw.queryCarsPrice(transactionId,st);
        mw.deleteCars(transactionId,st);

        mw.addRooms(transactionId,so,3,3000);
        mw.queryRooms(transactionId,so);
        mw.queryRoomsPrice(transactionId,so);
        mw.deleteRooms(transactionId,so);
    }
}