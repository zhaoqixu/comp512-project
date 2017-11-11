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
        int id = this.txn_counter;
        synchronized(this.active_txn) {
            // while (this.active_txn.containsKey(id) || id == 1) {
            //     id = new Random().nextInt(100000) + 1; //+1 because can return 0
            // }
            Transaction txn = new Transaction(id);
            TimeThread tt = new TimeThread(this, id);
            tt.start();
            this.active_txn.put(id, txn);
        }
        return id;
    }

    public boolean commit(int transactionId) 
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
                mw_locks.UnlockAll(transactionId);     
                this.active_txn.remove(transactionId);
            }
        }
        return true;
    }

    public void abort(int transactionId) throws RemoteException, InvalidTransactionException
    {
        synchronized(this.active_txn) {
            if (transactionId < 1 || !this.active_txn.containsKey(transactionId)) {
                Trace.warn("RM::Abort failed--Invalid transactionId");
                throw new InvalidTransactionException(transactionId);
            }
            else
            {
                Trace.info("RM::Aborting transaction : " + transactionId);
                mw_locks.UnlockAll(transactionId);
                this.active_txn.remove(transactionId);
            }
        }
    }

    public Stack getHistory(int transactionId)
    {
        synchronized(this.active_txn) {
            Transaction t = this.active_txn.get(transactionId);
            if (t!= null) return t.txn_hist;
            else return null;
        }
    }

    public boolean shutdown() throws RemoteException
    {
        synchronized(this.active_txn) {
            if (!this.active_txn.isEmpty()) {
                Trace.warn("RM::Shutdown failed--transaction active");
                return false;
            }
        }
        return true;
    }

    public boolean requestLock(int xid, String strData, int lockType)
    {
        synchronized(this.active_txn) {
            Transaction t = this.active_txn.get(xid);
            t.op_count++;
            this.active_txn.put(xid, t);
        }
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
    int time_to_live = 60000;

    public TimeThread (TransactionManager tm, int txnid) {
        this.tm = tm;
	    this.txnid = txnid;
    }

    public void run () {
        long start = System.currentTimeMillis();
        int old_count = 0;
        while(true){
            long elapsedTimeMills = System.currentTimeMillis()-start;
            if (elapsedTimeMills > time_to_live){
                try {
                    synchronized (tm.active_txn) {
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
                }
                catch (NullPointerException npe) {
                    return;
                }
            }
        }
    }
}
