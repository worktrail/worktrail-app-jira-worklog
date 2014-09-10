worktrail-app-jira-worklog

# WorkTrail Jira Worklog Sync

Synchronize WorkTrail Time Tracking work entries into the JIRA worklog. Sync is based
on providing the ticket number in a time tracking summary. You have to configure
ticket prefix. For example: use WT-123 in your work entry summaries.

See https://worktrail.net/ for details about WorkTrail's Time Tracking.

This app makes use of of the public API as documented at https://worktrail.net/en/api/


## Dependencies

https://github.com/worktrail/worktrail-app-api
https://github.com/worktrail/worktrail-app-hub-sync

For both run:

    ./gradlew publishMavenJavaPublicationToMavenLocal

## Development

    ./gradlew eclipse

## Running

see Launcher.java


# License

worktrail-app-jira-worklog is available under The BSD 2-Clause License. Please
contribute back by sending us pull requests on github:
https://github.com/worktrail/worktrail-app-jira-worklog
