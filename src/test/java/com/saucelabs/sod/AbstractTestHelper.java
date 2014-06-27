package com.saucelabs.sod;

import com.saucelabs.common.SauceOnDemandAuthentication;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.Before;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * @author Ross Rowe
 */
public abstract class AbstractTestHelper extends HttpServlet {

    public static final int PORT = 5000;
    protected static final String DEFAULT_SAUCE_DRIVER = "sauce-ondemand:?max-duration=60&os=windows 2008&browser=firefox&browser-version=30.";
    public static int code;
    private Server server;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");
        resp.getWriter().println("<html><head><title>test" + code + "</title></head><body>it works</body></html>");
    }

    @Before
    public void loadProperties() throws Exception {
        
        File sauceSettings = new File(new File(System.getProperty("user.home")), ".sauce-ondemand");
        if (!sauceSettings.exists()) {
            String userName = System.getProperty("sauce.user");
            String accessKey = System.getProperty("access.key");
            if (StringUtils.isNotBlank(userName) && StringUtils.isNotBlank(accessKey)) {
                SauceOnDemandAuthentication credential = new SauceOnDemandAuthentication(userName, accessKey);
                credential.saveTo(sauceSettings);
            }
        }
    }

    protected Server startWebServer() throws Exception {
        this.code = new Random().nextInt();

        // start the Jetty locally and have it respond our secret code.

        this.server = new Server(PORT);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        context.addServlet(new ServletHolder(this), "/*");

        server.start();
        System.out.println("Started Jetty at " + PORT);
        return server;
    }


}
