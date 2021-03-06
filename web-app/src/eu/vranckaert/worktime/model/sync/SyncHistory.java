package eu.vranckaert.worktime.model.sync;

import java.util.Date;

import com.google.appengine.api.datastore.Key;
import com.google.code.twig.annotation.Entity;
import com.google.code.twig.annotation.GaeKey;
import com.google.code.twig.annotation.Index;

@Entity(kind="syncHistory")
public class SyncHistory {
	@GaeKey private Key key;
	@Index private String userEmail;
	@Index private Date startTime;
	private Date endTime;

	private int incomingTimeRegistrations;
	private int incomingProjects;
	private int incomingTasks;
	private SyncConflictConfiguration conflictConfiguration;
	private int syncedTimeRegistrations;
	private int syncedProjects;
	private int syncedTasks;
	
	@Index private SyncResult syncResult;

	public Key getKey() {
		return key;
	}

	public void setKey(Key key) {
		this.key = key;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public int getIncomingTimeRegistrations() {
		return incomingTimeRegistrations;
	}

	public void setIncomingTimeRegistrations(int incomingTimeRegistrations) {
		this.incomingTimeRegistrations = incomingTimeRegistrations;
	}

	public int getIncomingProjects() {
		return incomingProjects;
	}

	public void setIncomingProjects(int incomingProjects) {
		this.incomingProjects = incomingProjects;
	}

	public int getIncomingTasks() {
		return incomingTasks;
	}

	public void setIncomingTasks(int incomingTasks) {
		this.incomingTasks = incomingTasks;
	}

	public SyncConflictConfiguration getConflictConfiguration() {
		return conflictConfiguration;
	}

	public void setConflictConfiguration(
			SyncConflictConfiguration conflictConfiguration) {
		this.conflictConfiguration = conflictConfiguration;
	}

	public int getSyncedTimeRegistrations() {
		return syncedTimeRegistrations;
	}

	public void setSyncedTimeRegistrations(int syncedTimeRegistrations) {
		this.syncedTimeRegistrations = syncedTimeRegistrations;
	}

	public int getSyncedProjects() {
		return syncedProjects;
	}

	public void setSyncedProjects(int syncedProjects) {
		this.syncedProjects = syncedProjects;
	}

	public int getSyncedTasks() {
		return syncedTasks;
	}

	public void setSyncedTasks(int syncedTasks) {
		this.syncedTasks = syncedTasks;
	}

	public SyncResult getSyncResult() {
		return syncResult;
	}

	public void setSyncResult(SyncResult syncResult) {
		this.syncResult = syncResult;
	}
}
