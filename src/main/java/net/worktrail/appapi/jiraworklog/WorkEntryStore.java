package net.worktrail.appapi.jiraworklog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.worktrail.appapi.model.EmployeeImpl;
import net.worktrail.appapi.model.WorkEntryImpl;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class WorkEntryStore {
	
	public long lastSync = 0;
	
	@JsonDeserialize(as=HashMap.class)
	public Map<Long, WorkEntryImpl> workEntries = new HashMap<>();
	
	@JsonDeserialize(as=ArrayList.class)
	public List<EmployeeImpl> employees = new ArrayList<>();
}
