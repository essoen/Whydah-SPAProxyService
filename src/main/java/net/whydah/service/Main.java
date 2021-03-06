package net.whydah.service;

import net.whydah.service.auth.UserAuthenticationResource;
import net.whydah.service.health.HealthResource;
import net.whydah.service.spasession.SPASessionResource;
import net.whydah.util.Configuration;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.web.context.ContextLoaderListener;

import java.util.logging.Level;
import java.util.logging.LogManager;

public class Main {
    public static final String CONTEXT_PATH = "/proxy";
    public static final String ADMIN_ROLE = "admin";
    public static final String USER_ROLE = "user";

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private Integer webappPort;
    private Server server;

    public Main() {
        this.server = new Server();
    }

    public Main withPort(Integer webappPort) {
        this.webappPort = webappPort;
        return this;
    }

    public static void main(String[] args) {
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        LogManager.getLogManager().getLogger("").setLevel(Level.INFO);

        Integer webappPort = Configuration.getInt("service.port");

        try {

            final Main main = new Main().withPort(webappPort);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    log.debug("ShutdownHook triggered. Exiting Whydah-SPAProxyService");
                    main.stop();
                }
            });

            main.start();
            log.debug("Finished waiting for Thread.currentThread().join()");
            main.stop();
        } catch (RuntimeException e) {
            log.error("Error during startup. Shutting down Whydah-SPAProxyService.", e);
            System.exit(1);
        }
    }

    public void start2() {
    	
    }
    
    // https://github.com/psamsotha/jersey-spring-jetty/blob/master/src/main/java/com/underdog/jersey/spring/jetty/JettyServerMain.java
    public void start() {
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath(CONTEXT_PATH);

        ConstraintSecurityHandler securityHandler = buildSecurityHandler();
        context.setSecurityHandler(securityHandler);

        ResourceConfig jerseyResourceConfig = new ResourceConfig();
        jerseyResourceConfig.packages("net.whydah");
        jerseyResourceConfig.register(org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature.class);
        jerseyResourceConfig.property(MvcFeature.TEMPLATE_BASE_PATH, "templates");
//        jerseyResourceConfig.register(MvcFeature.class);
        ServletHolder jerseyServlet = new ServletHolder(new ServletContainer(jerseyResourceConfig));
        context.addServlet(jerseyServlet, "/*");

        context.addEventListener(new ContextLoaderListener());

        context.setInitParameter("contextConfigLocation", "classpath:context.xml");

        ServerConnector connector = new ServerConnector(server);
        if (webappPort != null) {
            connector.setPort(webappPort);
        }

        if (Configuration.getBoolean("jetty.request.log.enabled")) {
            NCSARequestLog requestLog = buildRequestLog();
            server.setRequestLog(requestLog);
        }

        server.addConnector(connector);
        server.setHandler(context);

        try {
            server.start();
        } catch (Exception e) {
            log.error("Error during Jetty startup. Exiting", e);
            // "System. exit(2);"
        }
        webappPort = connector.getLocalPort();
        log.info("Whydah-SPAProxyService started on http://localhost:{}{}", webappPort, CONTEXT_PATH);
        try {
            server.join();
        } catch (InterruptedException e) {
            log.error("Jetty server thread when join. Pretend everything is OK.", e);
        }
    }

    private NCSARequestLog buildRequestLog() {
        NCSARequestLog requestLog = new NCSARequestLog("logs/jetty-yyyy_mm_dd.request.log");
        requestLog.setAppend(true);
        requestLog.setExtended(true);
        requestLog.setLogTimeZone("GMT");

        return requestLog;
    }

    private ConstraintSecurityHandler buildSecurityHandler() {
        Constraint userRoleConstraint = new Constraint();
        userRoleConstraint.setName(Constraint.__BASIC_AUTH);
        userRoleConstraint.setRoles(new String[]{USER_ROLE, ADMIN_ROLE});
        userRoleConstraint.setAuthenticate(true);

        Constraint adminRoleConstraint = new Constraint();
        adminRoleConstraint.setName(Constraint.__BASIC_AUTH);
        adminRoleConstraint.setRoles(new String[]{ADMIN_ROLE});
        adminRoleConstraint.setAuthenticate(true);

        ConstraintMapping clientConstraintMapping = new ConstraintMapping();
        clientConstraintMapping.setConstraint(userRoleConstraint);
        clientConstraintMapping.setPathSpec("/client/*");

        ConstraintMapping adminRoleConstraintMapping = new ConstraintMapping();
        adminRoleConstraintMapping.setConstraint(adminRoleConstraint);
        adminRoleConstraintMapping.setPathSpec("/admin/*");

        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        securityHandler.addConstraintMapping(clientConstraintMapping);
        securityHandler.addConstraintMapping(adminRoleConstraintMapping);

        // Allow healthresource to be accessed without authentication
        ConstraintMapping healthEndpointConstraintMapping = new ConstraintMapping();
        healthEndpointConstraintMapping.setConstraint(new Constraint(Constraint.NONE, Constraint.ANY_ROLE));
        healthEndpointConstraintMapping.setPathSpec(HealthResource.HEALTH_PATH);
        securityHandler.addConstraintMapping(healthEndpointConstraintMapping);

        // Allow proxyresource to be accessed without authentication
        ConstraintMapping proxyEndpointConstraintMapping = new ConstraintMapping();
        proxyEndpointConstraintMapping.setConstraint(new Constraint(Constraint.NONE, Constraint.ANY_ROLE));
        proxyEndpointConstraintMapping.setPathSpec(SPASessionResource.PROXY_PATH);
        securityHandler.addConstraintMapping(proxyEndpointConstraintMapping);

        // Allow userAuthEndpointConstraintMapping to be accessed without authentication
        ConstraintMapping userAuthEndpointConstraintMapping = new ConstraintMapping();
        userAuthEndpointConstraintMapping.setConstraint(new Constraint(Constraint.NONE, Constraint.ANY_ROLE));
        userAuthEndpointConstraintMapping.setPathSpec(UserAuthenticationResource.API_PATH + "/*");
        securityHandler.addConstraintMapping(userAuthEndpointConstraintMapping);

        HashLoginService loginService = new HashLoginService("Whydah-SPAProxyService");

        String clientUsername = Configuration.getString("login.user");
        String clientPassword = Configuration.getString("login.password");
        UserStore userStore = new UserStore();
        userStore.addUser(clientUsername, new Password(clientPassword), new String[]{USER_ROLE});

//        loginService.putUser(clientUsername, new Password(clientPassword), new String[]{USER_ROLE});
        loginService.setUserStore(userStore);

        String adminUsername = Configuration.getString("login.admin.user");
        String adminPassword = Configuration.getString("login.admin.password");
        userStore.addUser(adminUsername, new Password(adminPassword), new String[]{ADMIN_ROLE});
        loginService.setUserStore(userStore);
//        loginService.putUser(adminUsername, new Password(adminPassword), new String[]{ADMIN_ROLE});

        log.debug("Main instantiated with basic auth clientuser={} and adminuser={}", clientUsername, adminUsername);
        securityHandler.setLoginService(loginService);

        return securityHandler;
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            log.warn("Error when stopping Jetty server", e);
        }
    }

    public int getPort() {
        if (webappPort == null) {
            try {
                Thread.sleep(3000);
            } catch (Exception e) {
                log.error("Interrupted while waiting for jetty to start", e);
            }
        }

        return webappPort;
    }

    public boolean isStarted() {
        return server.isStarted();
    }
}
