package se.redpill.jboss.as5;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.authenticator.SingleSignOn;
import org.apache.catalina.authenticator.SingleSignOnEntry;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class CustomSingleSignOn extends SingleSignOn {

	public static final String ATTR_PRINCIPAL = "principal";
	public static final String ATTR_AUTHTYPE = "authType";
	public static final String ATTR_SESSIONID = "sessionId";	
	public static final String ATTR_USERNAME = "username";
	public static final String ATTR_PASSWORD = "password";
	public static final String DEFAULT_SESSIONID = "JSESSIONID";
	public static final String DEFAULT_AUTHTYPE = "NONE";	

	public void invoke(Request request, Response response) throws IOException, ServletException {

        request.removeNote(Constants.REQ_SSOID_NOTE);

        // Has a valid user already been authenticated?
        if (request.getUserPrincipal() != null) {
            getNext().invoke(request, response);
            return;
        }

        // Check for the single sign on cookie
        Cookie cookieSso = null;
        Cookie cookies[] = request.getCookies();
        if (cookies == null)
            cookies = new Cookie[0];
        for (int i = 0; i < cookies.length; i++) {
            if (Constants.SINGLE_SIGN_ON_COOKIE.equals(cookies[i].getName())) {
                cookieSso = cookies[i];
                break;
            }
        }
        
        if (cookieSso != null) {
            // Look up the cached Principal (in SingleSignOnEntry) associated with this cookie value
            SingleSignOnEntry entry = lookup(cookieSso.getValue());
            if (entry != null) {
                request.setNote(Constants.REQ_SSOID_NOTE, cookieSso.getValue());
                // Only set security elements if reauthentication is not required
                if (!getRequireReauthentication()) {
                    request.setAuthType(entry.getAuthType());
                    request.setUserPrincipal(entry.getPrincipal());
                }
            } else {
                cookieSso.setMaxAge(0);
                response.addCookie(cookieSso);
            }        	
        }

        // Invoke the next Valve in our pipeline
        getNext().invoke(request, response);
        
        //Perhaps custom authenticated has been made now by app...?
        if (request.getAttribute(ATTR_PRINCIPAL) != null) {
        	//Cache principal has been provided by application
        	//Find session cookie
        	Cookie cookieSession = null;
            for (int i = 0; i < cookies.length; i++) {
            	String sessionIdName = request.getAttribute(ATTR_SESSIONID) != null ? (String) request.getAttribute(ATTR_SESSIONID) : DEFAULT_SESSIONID;
                if (sessionIdName.equals(cookies[i].getName())) {
                    cookieSession = cookies[i];
                    break;
                }
            } 
            if (cookieSession != null) {
            	//Update sso cookie
    			cookieSso = new Cookie(Constants.SINGLE_SIGN_ON_COOKIE, cookieSession.getValue());
    			cookieSso.setPath("/");
    			response.addCookie(cookieSso);
            	//Register principal
    			String authType = request.getAttribute(ATTR_AUTHTYPE) != null ? (String) request.getAttribute(ATTR_AUTHTYPE) : DEFAULT_AUTHTYPE;
    			String username = request.getAttribute(ATTR_USERNAME) != null ? (String) request.getAttribute(ATTR_USERNAME) : null;
    			String password = request.getAttribute(ATTR_PASSWORD) != null ? (String) request.getAttribute(ATTR_PASSWORD) : null;    			
            	register(cookieSession.getValue(), (Principal) request.getAttribute(ATTR_PRINCIPAL), authType, username, password);
            	
            } else {
            	//Proceed, there's nothing to see here
            }
        }

	}
}
