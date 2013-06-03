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

    private BadgeManager mgr;
    public BadgeManager getManager() {
        if (mgr == null) {
            mgr = BadgeManager.getInstance();
        }
        return mgr;
    }

    /**
     * <p>Returns a JSON entity with the the types of badges provided by this authority.</p>
     * <p>For each domain, it provides the name of the badges it provides.</p>
     *
     * <div><b>METHOD: </b> GET</div>
     * <div><b>URL: </b>/badges/types</div>
     *
     * @return <p>Returns a JSON entity containing the following fields.</p>
     * <div>'errorCode': included only if there is an error. The value is an integer (currently, just 1)</div>
     * <div>'types': an array of types. Each element of the array is a map containing the following fields:</div>
     * <div> 'type': A string specifying the type of badge. The possible values are 'e' (email badge) and 'a' (inferrred
     * from the domain name.</div>
     * <div> 'domain': a string; the name of a domain (e.g., eweware.com)</div>
     * <div> 'badgeName': a string; the name of a badge provided by the domain (e.g., eweware.com
     * if it is an email type, or Tech Industry if it is an inferred type).</div>
     */
    @GET
    @Path("/types")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBadgeTypes() {
        try {
            return getManager().getBadgeTypes();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to provide badge types", e);
            return Response.serverError().build();
        }
    }

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
            final Response response = getManager().initBadgingTransaction(appName, appPassword);
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
        return getManager().processUserCredentials(txToken, email);
    }

    @POST
    @Path("/verify")
//    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN) // IE 8,9 will discard XCOR call if not plain text.
    public Response verify(
            @QueryParam("tk") String txToken,
            @QueryParam("c") String verificationCode) {
        return getManager().verify(txToken, verificationCode);
    }

    @POST
    @Path("/support")
    @Produces(MediaType.TEXT_PLAIN)
    public Response supportCall(
            @QueryParam("e") String userEmailAddress,
            @QueryParam("d") String domain
    ) {
//        logger.info("Received support call from " + userEmailAddress + " for domain " + domain);
        return getManager().handleSupportCall(userEmailAddress, domain);
    }
}

