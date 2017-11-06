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


public class Transaction
{
    protected int xid;
    public int op_count;
    protected Stack<Vector<String>> txn_hist = new Stack<Vector<String>>();

    public Transaction(int transactionId)
    {
        this.xid = transactionId;
    }

    public void addHistory(String... args)
    {
        Vector<String> v = new Vector();
        for (String arg : args)
        {
            v.addElement(arg);
        }
        this.txn_hist.push(v);
    }
}
