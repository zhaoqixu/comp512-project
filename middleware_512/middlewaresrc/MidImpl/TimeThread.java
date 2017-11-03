package MidImpl;

class TimeThread extends Thread {
    TransactionManager tm;
    int txnid;
    int time_to_live = 20000;


    public TimeThread (TransactionManager tm, int txnid) {
        this.tm = tm;
        this.txnid = txnid;
    }

    public void run () {
        int old_op_count = 0;
        // long start = System.currentTimeMillis();
        while(true){
            sleep(20000);
            // long elapsedTimeMills = System.currentTimeMillis()-start;
            if (elapsedTimeMills > time_to_live){
                if(tm.active_txn.get(txnid).op_count > old_op_count){
                    // start = System.currentTimeMillis();
                    old_op_count = tm.active_txn.get(txnid).op_count;
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
