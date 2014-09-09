package net.worktrail.appapi.jiraworklog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.worktrail.appapi.EmployeeListResponse;
import net.worktrail.appapi.WorkTrailAppApi;
import net.worktrail.appapi.jiraworklog.JiraSync.StoreWorkLogResponse;
import net.worktrail.appapi.model.Employee;
import net.worktrail.appapi.model.WorkEntry;
import net.worktrail.appapi.model.WorkEntryImpl;
import net.worktrail.appapi.response.RequestErrorException;
import net.worktrail.appapi.response.WorkEntryListResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


public class WorkEntrySync {
	
	private static final Logger logger = Logger.getLogger(WorkEntrySync.class.getName());

	private WorkTrailAppApi auth;

	private String jiraBaseUrl;

	private String jiraUsername;

	private String jiraPassword;

	private String[] workTrailEmails;

	private String prefixName;

	private List<Pattern> jiraIssueRegexes;
	
	private boolean dryRun = false;

	public WorkEntrySync(WorkTrailAppApi auth, String prefixName) {
		this.auth = auth;
		this.prefixName = prefixName;
	}

	public void runSync() {
		logger.info("We have to run sync.");
		readJiraConfig();
		File f = new File(prefixName + "_workentrystore.json");
		File workLogFile = new File(prefixName + "_worklogstore.json");
		WorkEntryStore workEntryStore = null;
		WorkLogSyncStore workLogSyncStore = null;
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		boolean doWorkTrailSync = true;
		try {
			if (f.exists()) {
				workEntryStore = mapper.readValue(f, WorkEntryStore.class);
			} else {
				workEntryStore = new WorkEntryStore();
			}
			
			if (workLogFile.exists()) {
				workLogSyncStore = mapper.readValue(workLogFile, WorkLogSyncStore.class);
			} else {
				workLogSyncStore = new WorkLogSyncStore();
			}
			
			syncEmployees(workEntryStore);
			
			if (doWorkTrailSync) {
				syncWorkEntries(workEntryStore);
				mapper.writeValue(f, workEntryStore);
			}
			
			try {
				syncWorkLog(workEntryStore, workLogSyncStore);
			} finally {
				mapper.writeValue(workLogFile, workLogSyncStore);
			}
		} catch (RequestErrorException | IOException e) {
			logger.log(Level.SEVERE, "Error while fetching work entries.", e);
		}
		
	}

	private void syncEmployees(WorkEntryStore workEntryStore) {
		EmployeeListResponse employeeList;
		try {
			employeeList = auth.fetchEmployees();
			workEntryStore.employees = new ArrayList<>(employeeList.getEmployeeList());
		} catch (RequestErrorException e) {
			throw new RuntimeException("Error fetching employees.", e);
		}
	}

	private void readJiraConfig() {
		String configFilePath = System.getProperty("net.worktrail.jira.config");
		final String errorMsg = "Please specify a path to a java system properties file referenced in the system property 'net.worktrail.jira.config' containing: jira.baseurl=... jira.username=... jira.password=... jira.issueregexes=(WT-\\\\d+),(DEV-\\\\d+) (comma separated list of regular expressions which match the ticket number at group $1) and worktrail.emails=... (comma separated list of emails of employees to sync)";
		if (configFilePath == null) {
			throw new RuntimeException("No system property defined. " + errorMsg);
		}
		File configFile = new File(configFilePath);
		if (!configFile.exists()) {
			throw new RuntimeException("config file not found. " + errorMsg);
		}
		Properties props = new Properties();
		try {
			props.load(new FileInputStream(configFile));
		} catch (IOException e) {
			throw new RuntimeException("Error loading config file. " + errorMsg, e);
		}
		this.jiraBaseUrl = props.getProperty("jira.baseurl");
		this.jiraUsername = props.getProperty("jira.username");
		this.jiraPassword = props.getProperty("jira.password");
		String jiraIssueRegexesString = props.getProperty("jira.issueregexes");
		String workTrailEmailsString = props.getProperty("worktrail.emails");
		
		if (jiraBaseUrl == null || jiraUsername == null || jiraPassword == null || workTrailEmailsString == null || jiraIssueRegexesString == null) {
			throw new RuntimeException("Missing property. " + errorMsg);
		}
		Pattern commaSplitter = Pattern.compile("\\s*,\\s*");
		this.workTrailEmails = commaSplitter.split(workTrailEmailsString);
		jiraIssueRegexes = commaSplitter.splitAsStream(jiraIssueRegexesString)
			.map((regex) -> Pattern.compile(regex, Pattern.CASE_INSENSITIVE))
			.collect(Collectors.toList());
		// all done, all good..
	}

	private void syncWorkLog(WorkEntryStore workEntryStore, WorkLogSyncStore workLogSyncStore) {
		// find valid work trail users ..
		List<String> workTrailEmailList = Arrays.asList(workTrailEmails);
		Map<Long, Employee> employeeById = workEntryStore.employees.stream()
			.filter((e) -> workTrailEmailList.contains(e.getPrimaryEmail()))
			.collect(Collectors.toMap((e) -> e.getEmployeeId(), (e) -> e));
		
		logger.info("Importing employees with id: " + employeeById);
		
		JiraSync jiraSync = new JiraSync(jiraBaseUrl, jiraUsername, jiraPassword);
		
		for(WorkEntry we : workEntryStore.workEntries.values()) {
			Employee employee = employeeById.get(we.getEmployeeId());
			if (employee == null) {
				continue;
			}
			for (Pattern p : jiraIssueRegexes) {
				Matcher matcher = p.matcher(we.getDescription());
				if (matcher.find()) {
					long duration = we.getEnd().getTime() - we.getStart().getTime();
					long durationSecs = duration / 1000;
					String jiraId = matcher.group(1);
					
					if (!workLogSyncStore.syncedEntries.containsKey(we.getId())) {
						logger.info("Need to sync to JIRA - id: {" + jiraId + "} duration: " + durationSecs + " in: " + we.getDescription());
						if (dryRun) {
							logger.info("Dryrun. Not sending to jira.");
						} else {
							StoreWorkLogResponse response = jiraSync.storeWorkLog(
									jiraId,
									"[WorkTrail:" + we.getId() + "] <"
											+ employee.getDisplayName() + ">: "
											+ we.getDescription(), durationSecs,
									we.getStart());
							workLogSyncStore.syncedEntries.put(
									we.getId(),
									new WorkLogSyncStore.WorkLogSyncEntry(
											we.getId(),
											jiraId,
											response.jiraWorkLogId, // jiraWorkLogId
											durationSecs
											));
						}
					}
				}
			}
		}
		logger.info("Synchronization done.");
	}

	private void syncWorkEntries(WorkEntryStore workEntryStore)
			throws RequestErrorException {
		long lastSync = workEntryStore.lastSync;
		long pageCount = 1;
		long newLastSync = lastSync;
		for (int i = 0 ; i < pageCount ; i++) {
			WorkEntryListResponse ret = auth.fetchWorkEntries(lastSync, i+1);
			pageCount = ret.getNumPages();
			logger.info("page " + (i+1) + " / " + ret.getNumPages() + " results:" + ret.getWorkEntryList().size());
			for (WorkEntry workEntry : ret.getWorkEntryList()) {
//				logger.info("  - " + workEntry.getDescription());
				newLastSync = Math.max(newLastSync, workEntry.getModifyDate());
				workEntryStore.workEntries.put(workEntry.getId(), (WorkEntryImpl) workEntry);
			}
			
//			workEntryStore.workEntries.addAll(ret.getWorkEntryList());
		}
		workEntryStore.lastSync = newLastSync;
	}
	


}
