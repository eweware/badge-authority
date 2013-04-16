package main.java.com.eweware.badging.mgr;

import main.java.com.eweware.badging.base.SystemErrorException;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.xml.ws.WebServiceException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

/**
 * <p>Manages verification emails sent to badge holders.</p>
 *
 * @author rk@post.harvard.edu
 *         Date: 3/8/13 Time: 1:24 PM
 */
public final class MailManager {

    private static MailManager singleton;

    public static MailManager getInstance() {
        return MailManager.singleton;
    }

    private ManagerState state = ManagerState.UNINITIALIZED;
    private Properties props = new Properties();
    private Session session;

    private final Boolean doNotActivate;
    private final String authorized;
    private final String tls;
    private final String hostname;
    private final String port;
    private final String account;
    private final String password;
    private final String from;

    public MailManager(Boolean doNotActivate,
                       String authorized,
                       String tls,
                       String hostname,
                       String port,
                       String account,
                       String password,
                       String from) {
        this.doNotActivate = doNotActivate;
        this.authorized = authorized;
        this.tls = tls;
        this.hostname = hostname;
        this.port = port;
        this.account = account;
        this.password = password;
        this.from = from;
        props.put("mail.smtp.auth", authorized);
        props.put("mail.smtp.starttls.enable", tls);
        props.put("mail.smtp.host", hostname);
        props.put("mail.smtp.port", port);
        printConfig();
        MailManager.singleton = this;
        state = ManagerState.INITIALIZED;
    }

    private void printConfig() {
        System.out.println("*** Start MailManager Properties ***");
        System.out.println(props);
        System.out.println("Account: " + account);
        System.out.println("Password: " + password);
        System.out.println("ReplyTo: " + from);
        System.out.println("*** End of MailManager Properties ***");
    }

    public Session getSession() {
        return session;
    }

    public String getFromEmailAddress() {
        return from;
    }

    /**
     * send method should really queue request to an smtp service *
     */
    public void send(String recipient, String subject, String body) throws SendFailedException, MessagingException {
        if (state != ManagerState.STARTED || recipient == null || subject == null || body == null) {
            System.out.println("WARNING: MailManager not sending");
            return;
        }
        final MimeMessage message = new MimeMessage(session);
        final MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

        message.setFrom(new InternetAddress(getFromEmailAddress()));
        //message.setReplyTo(new InternetAddress[]{new InternetAddress(getFromEmailAddress())});
        message.setRecipients(Message.RecipientType.TO, recipient);

        message.setSubject(subject);
        message.setContent(body, "text/html; charset=utf-8");
        Transport.send(message);
    }

    private void test() throws SystemErrorException {
        try {
            final boolean devMode = SystemManager.getInstance().isDevMode();
            final String host = InetAddress.getLocalHost().getHostName();
            final StringBuilder subject = new StringBuilder("Badge Authority Service ");
            subject.append(System.getProperty("user.name"));
            subject.append("@");
            subject.append(host);
            subject.append(" started ");
            subject.append(devMode ? " (DEVELOPMENT MODE) " : " (PRODUCTION) ");
            subject.append(new Date());
            final StringBuilder body = new StringBuilder("<div>The Badge Authority Service started on server domain <span style='color:red'>");
            body.append(host);
            body.append("</span></div><br/>");
            if (devMode) {
                body.append("<div style='color:red;font-weight:bold;'>Development Mode!</div><br/>");
            }
            body.append("<br/>");
            Properties props = System.getProperties();
            final Enumeration<?> elements = props.propertyNames();
            while (elements.hasMoreElements()) {
                final String pname = (String) elements.nextElement();
                body.append("<div>");
                body.append("<span style='color:green'>");
                body.append(pname);
                body.append("=");
                body.append("</span><span>");
                body.append(props.getProperty(pname));
                body.append("</span></div>");
            }
            body.append("<div/>");
            final String recipient = devMode ? "rk@eweware.com" : "rk@eweware.com, davevr@eweware.com";
            send(recipient, subject.toString(), body.toString());
        } catch (UnknownHostException e) {
            throw new SystemErrorException("Couldn't send email to unknown local host", e);
        } catch (MessagingException e) {
            throw new SystemErrorException("Failed to send email due to message error", e);
        }
    }

    public void start() {

        try {
            System.out.print("*** MailManager ");
            if (doNotActivate) {
                System.out.println("Disabled ***");
                return;
            }
            System.out.println("Enabled ***");
            session = Session.getInstance(props,
                    new Authenticator() {
                        @Override
                        public PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(account, password);
                        }
                    });
            state = ManagerState.STARTED;

            if (!doNotActivate) {
                test();
            }
            System.out.println("*** MailManager started ***");
        } catch (Exception e) {
            throw new WebServiceException("Failed to start mail manager", e);
        }
    }

    public void shutdown() {
        session = null;
        state = ManagerState.SHUTDOWN;
        System.out.println("*** MailManager shut down ***");
    }


}
