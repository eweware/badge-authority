package main.java.com.eweware.badging.rest.resource;

import main.java.com.eweware.badging.mgr.BadgeManager;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author rk@post.harvard.edu
 *         Date: 3/19/13 Time: 1:40 PM
 */
@Path("badges")
public class BadgesResource {

    private static final Logger logger = Logger.getLogger("BadgesResource");

    /**
     * <p>The badge requestor (an app) uses this to provide its credentials and to request that a badge
     * be created.</p>
     * <p>A session id is created on behalf of the ultimate user whose badge is created. An HTML5
     * form is returned by this request. This form will ask the user to submit his/her email
     * address. The form will be sent directly to this (the badge authority) server's @POST badges/make method
     * using the supplied session id.</p>
     *
     * @param entity A JSON entity containing the following fields:
     *               <div>'a' := the application name</div>
     *               <div>'p':= the application password</div>
     * @return Returns an HTML string representing a form to be directly submitted
     *         to this server. The form requests an email address from the user.
     *         It is the responsibility of the app (server) to : (1) pass this string
     *         to the ultimate consumer (a user at a browser), and (2)
     *         pass the session cookie to the ultimate user's browser.
     */
    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response initiateBadgeCreation(
            Map<String, Object> entity
    ) {
        final String appName = (String) entity.get("a");
        final String appPassword = (String) entity.get("p");
        try {
            final Response response = BadgeManager.getInstance().initBadgingTransaction(appName, appPassword);
            return response;
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return Response.serverError().build();
        }
    }

    @POST
    @Path("/credentials")
//    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN) // IE 8,9 will discard XCOR call if not plain text.
    public Response processUserCredentials(
            @QueryParam("tk") String txToken,
            @QueryParam("e") String email
//            @Context HttpServletRequest request
    ) {

//        final String requestURI = request.getRequestURI();
//        final StringBuffer requestURL = request.getRequestURL();
        return BadgeManager.getInstance().processUserCredentials(txToken, email);
    }

    @POST
    @Path("/verify")
//    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN) // IE 8,9 will discard XCOR call if not plain text.
    public Response verify(
            @QueryParam("tk") String txToken,
            @QueryParam("c") String verificationCode) {
        return BadgeManager.getInstance().verify(txToken, verificationCode);
    }

    @POST
    @Path("/support")
    @Produces(MediaType.TEXT_PLAIN)
    public Response supportCall(
            @QueryParam("e") String userEmailAddress,
            @QueryParam("d") String domain
    ) {
        logger.info("Received support call from " + userEmailAddress + " for domain " + domain);
        return BadgeManager.getInstance().handleSupportCall(userEmailAddress, domain);
    }
}

