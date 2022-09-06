package org.opennms.horizon.minion.weblisten;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.opennms.horizon.minion.plugin.api.Listener;
import org.opennms.horizon.minion.plugin.api.ParameterMap;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponseImpl;
import org.opennms.weblisten.contract.WebListenRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.function.Consumer;

/**
 * PROTOTYPE
 */
public class WebListener implements Listener {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(WebListener.class);

    private Logger log = DEFAULT_LOGGER;

    private Consumer<ServiceMonitorResponse> resultProcessor;
    private WebListenRequest request;
    private Server server;

    public WebListener(Consumer<ServiceMonitorResponse> resultProcessor, WebListenRequest request) {
        this.resultProcessor = resultProcessor;
        this.request = request;
    }

//========================================
// Collector API
//----------------------------------------

    @Override
    public void start() throws Exception {
        server = prepareJettyServer(request.getAddress(), request.getPort());
        server.start();
    }

    @Override
    public void stop() {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception exception) {
                log.error("ERROR ON STOP", exception);
            }
        }
    }

//========================================
// Internals
//----------------------------------------

    private Server prepareJettyServer(String listenAddress, int port) {
        InetSocketAddress inetSocketAddress = new InetSocketAddress(listenAddress, port);

        Server result = new Server(inetSocketAddress);

        ServletContextHandler rootHandler = this.prepareRootContextHandler(result);
        result.setHandler(rootHandler);

        return result;
    }

    private ServletContextHandler prepareRootContextHandler(Server server) {
        ServletContextHandler result = new ServletContextHandler();

        ServletHolder servletHolder = new ServletHolder();
        servletHolder.setServlet(new MyServlet());

        result.addServlet(servletHolder, "/*");

        return result;
    }

//========================================
// Servlet Implementation
//----------------------------------------

    private class MyServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            log.info("GOT A POST REQUEST: uri=" + req.getRequestURI());

            ServiceMonitorResponse response =
                    ServiceMonitorResponseImpl.builder()
                        .status(ServiceMonitorResponse.Status.Up)
                        .reason("endpoint " + req.getRequestURI())
                        .build()
                    ;

            resultProcessor.accept(response);

            resp.setStatus(HttpServletResponse.SC_OK);
        }
    }
}
