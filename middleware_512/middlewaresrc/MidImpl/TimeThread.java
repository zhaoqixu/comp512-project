package MidImpl;

class TimeThread extends Thread {
    TransactionManager tm;
    int txnid;
    int time_to_live = 30000;

    public TimeThread (TransactionManager tm, int txnid) {
        this.tm = tm;
	    this.txnid = txnid;
    }

    public void run () {
        long start = System.currentTimeMillis();
        while(true){
            long elapsedTimeMills = System.currentTimeMillis()-start;
            if (elapsedTimeMills > time_to_live){
                if(tm.active_txn.get(txnid).op_count > 0){
                    start = System.currentTimeMillis();
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
    }
}
