package main.java.com.eweware.badging.rest.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author rk@post.harvard.edu
 *         Date: 3/19/13 Time: 1:40 PM
 */
@Path("badges")
public class BadgesResource {

    @GET
    @Path("/{userToken}")
    @Produces(MediaType.TEXT_HTML)
    public Response createBadge() {
        final String data = "<b>Hello there!</b>";
        return Response.ok(data).build();
    }
}
