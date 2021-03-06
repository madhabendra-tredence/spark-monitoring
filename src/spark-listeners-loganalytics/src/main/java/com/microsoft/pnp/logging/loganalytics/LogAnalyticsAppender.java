package com.microsoft.pnp.logging.loganalytics;

import com.microsoft.pnp.LogAnalyticsEnvironment;
import com.microsoft.pnp.client.loganalytics.LogAnalyticsClient;
import com.microsoft.pnp.client.loganalytics.LogAnalyticsSendBufferClient;
import com.microsoft.pnp.logging.JSONLayout;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

import static com.microsoft.pnp.logging.JSONLayout.TIMESTAMP_FIELD_NAME;

public class LogAnalyticsAppender extends AppenderSkeleton {
    private static final Filter ORG_APACHE_HTTP_FILTER = new Filter() {
        @Override
        public int decide(LoggingEvent loggingEvent) {
            if (loggingEvent.getLoggerName().startsWith("org.apache.http")) {
                return Filter.DENY;
            }

            return Filter.NEUTRAL;
        }
    };

    private static final String DEFAULT_LOG_TYPE = "SparkLoggingEvent";
    // We will default to environment so the properties file can override
    private String workspaceId = LogAnalyticsEnvironment.getWorkspaceId();
    private String secret = LogAnalyticsEnvironment.getWorkspaceKey();
    private String logType = DEFAULT_LOG_TYPE;
    private LogAnalyticsSendBufferClient client;

    public LogAnalyticsAppender() {
        this.addFilter(ORG_APACHE_HTTP_FILTER);
        // Add a default layout so we can simplify config
        this.setLayout(new JSONLayout());
    }

    @Override
    public void activateOptions() {
        this.client = new LogAnalyticsSendBufferClient(
                new LogAnalyticsClient(this.workspaceId, this.secret),
                this.logType
        );
    }

    @Override
    protected void append(LoggingEvent loggingEvent) {
        try {
            String json = this.getLayout().format(loggingEvent);
            this.client.sendMessage(json, TIMESTAMP_FIELD_NAME);
        } catch (Exception ex) {
            LogLog.error("Error sending logging event to Log Analytics", ex);
        }
    }

    @Override
    public boolean requiresLayout() {
        // We will set this to false so we can simplify our config
        // If no layout is provided, we will get the default.
        return false;
    }

    @Override
    public void close() {
        this.client.close();
    }

    @Override
    public void setLayout(Layout layout) {
        // This will allow us to configure the layout from properties to add custom JSON stuff.
        if (!(layout instanceof JSONLayout)) {
            throw new UnsupportedOperationException("layout must be an instance of JSONLayout");
        }

        super.setLayout(layout);
    }

    @Override
    public void clearFilters() {
        super.clearFilters();
        // We need to make sure to add the filter back so we don't get stuck in a loop
        this.addFilter(ORG_APACHE_HTTP_FILTER);
    }

    public String getWorkspaceId() {
        return this.workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getSecret() {
        return this.secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getLogType() {
        return this.logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }
}
