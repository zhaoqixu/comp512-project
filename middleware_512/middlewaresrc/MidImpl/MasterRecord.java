package MidImpl;

import java.io.Serializable;

public class MasterRecord implements Serializable {
	private int activeDb = 0;
	private int lastXid;

	public int getCommittedIndex() {
		return activeDb;
	}
	
	public int getWorkingIndex() {
		return 1 - activeDb;
	}
	
	public void setCommittedIndex(int committedIndex) {
		this.activeDb = committedIndex;
	}
	
	public int getLastXid() {
		return lastXid;
	}
	
	public void setLastXid(int lastXid) {
		this.lastXid = lastXid;
	}
	
	public void swap() {
		activeDb = 1 - activeDb;
	}
}