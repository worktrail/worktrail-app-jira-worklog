package net.worktrail.appapi.jiraworklog;

import net.worktrail.appapi.WorkTrailAccessType;
import net.worktrail.appapi.WorkTrailAppApi;
import net.worktrail.appapi.WorkTrailScope;
import net.worktrail.appapi.hub.WorkTrailCliFramework;
import net.worktrail.appapi.hub.WorkTrailSync;
import net.worktrail.appapi.hub.git.SyncStorage;

/**
 * cli for our jira worklog sync.
 *  we only extend WorkTrailCliFramework to get the "authentication" framework.
 * @author herbert
 */
public class WorklogSyncCli extends WorkTrailCliFramework {
	
	private String prefixName;

	public WorklogSyncCli() {
	}
	
	public String getPrefixName() {
		if (prefixName != null) {
			return prefixName;
		}
		this.prefixName = System.getProperty("net.worktrail.jira.prefix");
		if (prefixName == null || prefixName.length() < 1) {
			throw new RuntimeException("set system property net.worktrail.jira.prefix to a prefix for configuration/save files.");
		}
		return prefixName;
	}
	
	@Override
	protected void executeFromCommandline(String[] args) {
		if (!hasAuthentication()) {
			authenticate();
		}
		WorkEntrySync sync = new WorkEntrySync(getAuth(), prefixName);
		sync.runSync();
	}

	@Override
	protected WorkTrailSync createSyncObject(SyncStorage syncStorage,
			WorkTrailAppApi api, String[] args) {
		return null;
	}

	@Override
	protected String getSyncUnixName() {
		return getPrefixName() + "_jiraworklog";
	}

	

	@Override
	protected WorkTrailScope[] getAuthScopes() {
		return new WorkTrailScope[] {
				WorkTrailScope.READ_EMPLOYEES,
				WorkTrailScope.SYNC_HUB_DATA,
				WorkTrailScope.READ_WORKENTRIES,
				WorkTrailScope.READ_TASKS };
	}
	
	@Override
	protected WorkTrailAccessType getAccessType() {
		return WorkTrailAccessType.COMPANY;
	}
}
