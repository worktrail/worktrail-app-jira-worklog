package net.worktrail.appapi.jiraworklog;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class WorkLogSyncStore {
	
	public static class WorkLogSyncEntry {
		public Long workEntryId;
		public String jiraId;
		public String jiraWorkLogId;
		public long durationSecs;
		
		public WorkLogSyncEntry() {
		}
		
		public WorkLogSyncEntry(Long workEntryId, String jiraId,
				String jiraWorkLogId, long durationSecs) {
			super();
			this.workEntryId = workEntryId;
			this.jiraId = jiraId;
			this.jiraWorkLogId = jiraWorkLogId;
			this.durationSecs = durationSecs;
		}
		
		
	}
	
	@JsonDeserialize(as=HashMap.class)
	public Map<Long, WorkLogSyncEntry> syncedEntries = new HashMap<Long, WorkLogSyncStore.WorkLogSyncEntry>();
}
