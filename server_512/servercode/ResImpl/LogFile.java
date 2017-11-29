package ResImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class LogFile implements Serializable {
    public int txnid = 0;
    public ArrayList<String> record = new ArrayList<String>();
    public LogFile(int transactionId){
        this.txnid = transactionId;
    }
}