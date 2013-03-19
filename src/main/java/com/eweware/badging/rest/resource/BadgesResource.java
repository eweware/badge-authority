package main.java.com.eweware.badging.rest.resource;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * @author rk@post.harvard.edu
 *         Date: 3/19/13 Time: 1:40 PM
 */
@Path("badges")
public class BadgesResource {

    public Response createBadge() {
        return Response.ok().build();
    }
}
