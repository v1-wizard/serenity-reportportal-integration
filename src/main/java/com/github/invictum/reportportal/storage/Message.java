package com.github.invictum.reportportal.storage;

import com.epam.reportportal.message.ReportPortalMessage;
import com.github.invictum.reportportal.MessageLevel;

import java.util.Calendar;
import java.util.Date;

/**
 * Wrapper around {@link ReportPortalMessage} class. Stores all date required for log emitting.
 */
public class Message {

    private ReportPortalMessage body;
    private String message;
    private Date date = Calendar.getInstance().getTime();
    private MessageLevel level = MessageLevel.INFO;

    public Message(ReportPortalMessage body) {
        this.body = body;
    }

    public Message(String message) {
        this.message = message;
    }

    public ReportPortalMessage getBody() {
        return body;
    }

    public void setBody(ReportPortalMessage body) {
        this.body = body;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public MessageLevel getLevel() {
        return level;
    }

    public void setLevel(MessageLevel level) {
        this.level = level;
    }
}
