package main.java.com.eweware.badging;

import com.sun.jersey.api.core.PackagesResourceConfig;

/**
 * <p>Specifies package where REST resource classes reside.</p>
 *
 * @author rk@post.harvard.edu
 *         Date: 3/8/13 Time: 1:40 PM
 */
public class App extends PackagesResourceConfig {

    public App() {
        // All REST resource classes must be in the following package:
        super("main.java.com.eweware.badging.rest.resource");
    }
}
