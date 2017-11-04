package MidImpl;
import ResInterface.*;
import jdk.nashorn.internal.ir.RuntimeNode.Request;
import MidInterface.*;
import LockManager.*;

import java.util.*;
// import java.io.PrintWriter;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RMISecurityManager;

public class TransactionManager
{
    public static final int READ = 0;
    public static final int WRITE = 1;
    protected static LockManager mw_locks = new LockManager();
    protected static int txn_counter = 0;
    public static Hashtable<Integer,Transaction> active_txn = new Hashtable<Integer, Transaction>();

    public int start() throws RemoteException {
        this.txn_counter ++;
        Transaction txn = new Transaction(txn_counter);
        TimeThread tt = new TimeThread(this, txn_counter);
        tt.start();
        this.active_txn.put(txn_counter, txn);
        return txn_counter;
    }

    public boolean commit(int transactionId) 
        throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        if (transactionId < 1 || !this.active_txn.containsKey(transactionId)) {
            Trace.warn("RM::Commit failed--Invalid transactionId");
            throw new InvalidTransactionException(transactionId);
        }
        else
        {
            Trace.info("RM::Committing transaction : " + transactionId);
            mw_locks.UnlockAll(transactionId);     
            active_txn.remove(transactionId);
        }
        return true;
    }

    public void abort(int transactionId) throws RemoteException, InvalidTransactionException
    {
        if (transactionId < 1 || !this.active_txn.containsKey(transactionId)) {
            Trace.warn("RM::Abort failed--Invalid transactionId");
            throw new InvalidTransactionException(transactionId);
        }
        else
        {
            Trace.info("RM::Aborting transaction : " + transactionId);
            //TODO recovery
            mw_locks.UnlockAll(transactionId);
            active_txn.remove(transactionId);
        }
    }

    public boolean shutdown() throws RemoteException
    {
        if (!active_txn.isEmpty()) {
            Trace.warn("RM::Shutdown failed--transaction active");
            return false;
        }
        else
        {
            /* TODO: store data? */
            // if (!MiddleWareImpl.rm_car.shutdown()) return false;
            // if (!rm_room.shutdown()) return false;
            // if (!rm_flight.shutdown()) return false;
            // 
        }
        return true;
    }

    public boolean requestLock(int xid, String strData, int lockType)
    {
        Transaction t = active_txn.get(xid);
        t.op_count++;
        active_txn.put(xid, t);
        //System.out.println(active_txn.get(xid).op_count);
        try {
            return mw_locks.Lock(xid, strData, lockType);
        }
        catch (DeadlockException dle) {
            Trace.warn("RM::Lock failed--Deadlock exist");
            try {
            abort(xid);
            }
            catch (Exception e){
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
            Trace.warn("RM::Transaction ID aborted");
            return false;
        }
    }
}

class TimeThread extends Thread {
    TransactionManager tm;
    int txnid;
    int time_to_live = 30000;

    public TimeThread (TransactionManager tm, int txnid) {
        this.tm = tm;
	    this.txnid = txnid;
    }

    public void run () {
        // try{
        long start = System.currentTimeMillis();
        // PrintWriter writer = new PrintWriter("trace.txt", "UTF-8");
        int old_count = 0;
        while(true){
            long elapsedTimeMills = System.currentTimeMillis()-start;
            if (elapsedTimeMills > time_to_live){
                // writer.println("------------------------------------");                
                // writer.println(tm.active_txn.get(txnid).op_count);
                // writer.println(old_count);                
                // writer.println(elapsedTimeMills);
                try {
                    if(tm.active_txn.get(txnid).op_count > old_count){
                        start = System.currentTimeMillis();
                        old_count = tm.active_txn.get(txnid).op_count;
                    }
                    else{
                        try {
                            tm.abort(txnid); //time out
                            break;
                        }
                        catch (Exception e)
                        {
                            System.out.println(e.getMessage());
                        }
                    }
                }
                catch (NullPointerException npe) {
                    return;
                }
            }
        }
        // writer.close();
    // }
    // catch(Exception e){}
    }
}
