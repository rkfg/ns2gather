package me.rkfg.ns2gather.server;

import java.io.IOException;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import me.rkfg.ns2gather.client.CookieSettingsManager;
import me.rkfg.ns2gather.domain.Remembered;

import org.hibernate.Session;
import org.openid4java.association.AssociationException;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.MessageException;
import org.openid4java.message.ParameterList;

import ru.ppsrk.gwt.client.ClientAuthException;
import ru.ppsrk.gwt.client.LogicException;
import ru.ppsrk.gwt.server.HibernateCallback;
import ru.ppsrk.gwt.server.HibernateUtil;

public class AuthCallbackServlet extends HttpServlet {

    /**
     * 
     */
    private static final long serialVersionUID = 6146495372925195290L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // extract the parameters from the authentication response
            // (which comes in as a HTTP request from the OpenID provider)
            ParameterList openidResp = new ParameterList(req.getParameterMap());

            // retrieve the previously stored discovery information
            DiscoveryInformation discovered = (DiscoveryInformation) req.getSession().getAttribute("discovered");

            // extract the receiving URL from the HTTP request
            StringBuffer receivingURL = req.getRequestURL();
            String queryString = req.getQueryString();
            if (queryString != null && queryString.length() > 0)
                receivingURL.append("?").append(req.getQueryString());

            // verify the response
            VerificationResult verification = NS2GServiceImpl.manager.verify(receivingURL.toString(), openidResp, discovered);

            // examine the verification result and extract the verified identifier
            Identifier verified = verification.getVerifiedId();

            if (verified != null) {
                try {
                    Long steamId = Long.valueOf(verified.getIdentifier().replaceAll("http://steamcommunity.com/openid/id/", ""));
                    req.getSession().setAttribute(Settings.STEAMID_SESSION, steamId);
                    updateRememberCookie(resp, rememberMe(steamId));
                    resp.sendRedirect("..");
                } catch (NumberFormatException e) {
                    resp.getWriter().print("Получен нечисловой Steam ID. GABEN PLZ!");
                }
            } else {
                resp.getWriter().print("fail!");
            }
        } catch (MessageException | DiscoveryException | AssociationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void updateRememberCookie(HttpServletResponse resp, String rememberIdStr) {
        Cookie rememberMeCookie = new Cookie(CookieSettingsManager.REMEMBER_STEAM_ID, rememberIdStr);
        rememberMeCookie.setMaxAge((int) CookieSettingsManager.COOKIE_AGE);
        resp.addCookie(rememberMeCookie);
    }

    private String rememberMe(final Long steamId) {
        try {
            return HibernateUtil.exec(new HibernateCallback<String>() {

                @Override
                public String run(Session session) throws LogicException, ClientAuthException {
                    Long rememberId = new Random().nextLong();
                    Remembered remembered = new Remembered(rememberId, steamId);
                    session.merge(remembered);
                    return rememberId.toString();
                }
            });
        } catch (LogicException | ClientAuthException e) {
            e.printStackTrace();
        }
        return "";
    }
}
