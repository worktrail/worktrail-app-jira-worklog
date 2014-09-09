package net.worktrail.appapi.jiraworklog;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.io.BaseEncoding;

/**
 * Simple jira sync, currently only supports basic AUTH.
 * 
 * Problem: https://jira.atlassian.com/browse/JRA-30197
 * API doc: https://docs.atlassian.com/jira/REST/latest/
 * 
 * @author herbert
 */
public class JiraSync {
	
	private static final Logger logger = Logger.getLogger(JiraSync.class.getName());
	
	private String baseUrl;
	private String username;
	private String password;

	public JiraSync() {
		this.baseUrl = getConfig("jira_baseurl");
		this.username = System.getProperty("jira_username");
		this.password = System.getProperty("jira_password");
	}
	
	private String getConfig(String configName) {
		String property = "net.worktrail." + configName;
		String value = System.getProperty(property);
		if (value == null) {
			throw new IllegalStateException("Missing argument/system property: " + property);
		}
		return value;
	}

	public JiraSync(String baseUrl, String username, String password) {
		this.baseUrl = baseUrl;
		this.username = username;
		this.password = password;
	}
	
	public static class StoreWorkLogResponse {
		String jiraWorkLogId;
		String jiraWorkLogUrl;
	}
	
	public StoreWorkLogResponse storeWorkLog(String jiraIssueId, String comment, long durationSeconds, Date started) {
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("comment", comment);
			jsonObject.put("timeSpentSeconds", durationSeconds);
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
			jsonObject.put("started", format.format(started));
		} catch (JSONException e) {
			throw new RuntimeException("Error creating json object.", e);
		}
		JSONObject response = sendPost("/rest/api/2/issue/" + jiraIssueId + "/worklog", jsonObject);
		StoreWorkLogResponse workLogResponse = new StoreWorkLogResponse();
		try {
			workLogResponse.jiraWorkLogId = response.getString("id");
			workLogResponse.jiraWorkLogUrl = response.getString("self");
		} catch (JSONException e) {
			try {
				logger.log(Level.SEVERE, "Error while parsing response. {" + response.toString(2) + "}", e);
			} catch (JSONException e1) {
				logger.log(Level.SEVERE, "Error while parsing response.", e);
			}
		}
		return workLogResponse;
	}
	
	private JSONObject sendPost(String url, JSONObject jsonObject) {
		try {
			URL reqUrl = new URL(baseUrl + url);
			String body = jsonObject.toString(2);
			logger.info("Sending request to " + reqUrl + " with body: {" + body + "}");
			String response = requestDataFromUrl(reqUrl, body.getBytes("UTF-8"), null);
			return new JSONObject(response);
		} catch (JSONException | IOException e) {
			throw new RuntimeException("Error sending request to " + url, e);
		}
	}
	
	
	
	private String requestDataFromUrl(URL url, byte[] tosend, String userAgent) throws IOException {
		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
		if (userAgent != null) {
			urlConnection.setRequestProperty("User-Agent", userAgent);
		}
		if (username != null && password != null) {
			String userpass = username + ":" + password;
			String basicAuth = "Basic " + BaseEncoding.base64().encode(userpass.getBytes());
			urlConnection.setRequestProperty("Authorization", basicAuth);
		}
		urlConnection.addRequestProperty("Content-Type", "application/json");
		urlConnection.setDoOutput(true);
		urlConnection.setDoInput(true);
		urlConnection.setConnectTimeout(5000);
		urlConnection.setReadTimeout(20000);
		// urlConnection.setChunkedStreamingMode(0);
		urlConnection.setFixedLengthStreamingMode(tosend.length);

		BufferedOutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
		out.write(tosend);
		out.flush();
		out.close();
		
		int responseCode = urlConnection.getResponseCode();
		logger.info("Got responseCode: " + responseCode);

		InputStreamReader reader = new InputStreamReader(urlConnection.getInputStream());
		StringBuilder sb = new StringBuilder();
		char[] buf = new char[2048];
		int i;
		while ((i = reader.read(buf)) != -1) {
			sb.append(buf, 0, i);
		}
		logger.info("done reading. " + sb.length());
		return sb.toString();
	}
}
