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

public class TransactionManager
{
    public static final int READ = 0;
    public static final int WRITE = 1;
    protected LockManager mw_locks;
    protected LockManager car_locks;
    protected LockManager flight_locks;
    protected LockManager room_locks;
    protected int txn_counter; // should be moved to TM
    protected Hashtable<Integer,Transaction> active_txn; // should be moved to TM

    public TransactionManager()
    {
        this.mw_locks = new LockManager();
        this.car_locks = new LockManager();
        this.flight_locks = new LockManager();
        this.room_locks = new LockManager();
        this.txn_counter = 0;
        this.active_txn = new Hashtable<Integer, Transaction>();
    }

    public int start() throws RemoteException {
        this.txn_counter ++;
        Transaction txn = new Transaction(txn_counter);
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
            if (!flight_locks.UnlockAll(transactionId)) {
                return false;
            }
            if (!car_locks.UnlockAll(transactionId)) {
                // recover rm_flight
                return false;
            }
            if (!room_locks.UnlockAll(transactionId)) {
                // recover rm_flight
                // recover rm_car
                return false;
            }
            if (!mw_locks.UnlockAll(transactionId)) {
                return false;
            }
            active_txn.remove(transactionId);
        }
        return true;
    }

    public void abort(int transactionId) throws RemoteException, InvalidTransactionException
    {
        if (transactionId < 1 || !this.active_txn.containsKey(transactionId)) {
            Trace.warn("RM::Commit failed--Invalid transactionId");
            throw new InvalidTransactionException(transactionId);
        }
        else {
            Trace.info("RM::Committing transaction : " + transactionId);
            if (!flight_locks.UnlockAll(transactionId)) {
            }
            if (!car_locks.UnlockAll(transactionId)) {
                // recover rm_flight
            }
            if (!room_locks.UnlockAll(transactionId)) {
                // recover rm_flight
                // recover rm_car
            }
            if (!mw_locks.UnlockAll(transactionId)) {
            }
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
            // if (!rm_car.shutdown()) return false;
            // if (!rm_room.shutdown()) return false;
            // if (!rm_flight.shutdown()) return false;
        }
        return true;
    }
}