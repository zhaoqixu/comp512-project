package ResImpl;

/*
    The transaction id is invalid. 
*/

public class InvalidTransactionException extends Exception
{
    private int xid = 0;
    
    public InvalidTransactionException (int xid)
    {
        super("The transaction id, " + xid + ", is invalid");
        this.xid = xid;
    }
    
    int GetXId()
    {
        return xid;
    }
}