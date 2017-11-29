package MidImpl;

public class CrashException extends Exception {
    private int flag = 0;
    
    public CrashException (int flag)
    {
        super("Crashed with flag : " + flag);
        this.flag = flag;
    }
    
    int getFlag()
    {
        return flag;
    }
}