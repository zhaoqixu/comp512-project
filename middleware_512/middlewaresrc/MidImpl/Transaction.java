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

    public Transaction(int transactionId)
    {
        this.xid = transactionId;
    }
}
