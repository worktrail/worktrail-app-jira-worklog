package net.worktrail.appapi.jiraworklog;

public class Launcher {
	public static void main(String[] args) {
		new WorklogSyncCli().executeFromCommandline(args);
	}
}
