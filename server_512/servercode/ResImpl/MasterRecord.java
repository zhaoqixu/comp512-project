package ResImpl;

import java.io.Serializable;

public class MasterRecord implements Serializable {
	private int activeCopy = 0;
	private int lastXid;

	public int getCommittedIndex() {
		return activeCopy;
	}
	
	public int getWorkingIndex() {
		return 1 - activeCopy;
	}
	
	public void setCommittedIndex(int committedIndex) {
		this.activeCopy = committedIndex;
	}
	
	public int getLastXid() {
		return lastXid;
	}
	
	public void setLastXid(int lastXid) {
		this.lastXid = lastXid;
	}
	
	public void swap() {
		activeCopy = 1 - activeCopy;
	}
}