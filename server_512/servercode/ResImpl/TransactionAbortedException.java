package ResImpl;

/*
    The transaction id is aborted. 
*/

public class TransactionAbortedException extends Exception
{
    private int xid = 0;
    
    public TransactionAbortedException (int xid)
    {
        super("The transaction id, " + xid + ", is aborted");
        this.xid = xid;
    }
    
    int GetXId()
    {
        return xid;
    }
}