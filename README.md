JBossWebCustomSigneSignOnValve
==============================

A JBossWeb valve that extends SingleSignOn valve and enables web apps like Seam with non-custom authentication to store that principal object needed for SSO in other web-apps running on the same instance/host. This valve extends the "standard" SingleSignOn valve, so be sure to check out the documentation at https://community.jboss.org/wiki/JBossWebSingleSignOn

Thus, you may easily create a "master" web app, e.g a Seam app that uses SAML2 for authentication, and "slave" web apps, that by adding a standard JEE security constraint in web.xml can rescrict usage to users that are already logged in.
So split up big web-apps into smaller ones and enjoy the benifits of modularity - you can redeploy slave apps without forcing users to log in again.

0.9 version has been tested on
* JBoss AS 5.1 (JBossWeb 2.1.3.Final), non-clustered

## Build ##
Notting fancy, clone and build this rep

	git clone git://github.com/jensaug/jbossweb-customsso.git
	cd jbossweb-customsso
	mvn clean install

## Install ##
* Copy ```target/customSso.jar``` to ```[JBOSS_HOME]/server/default/lib```
* Edit ```[JBOSS_HOME]/server/default/deploy/jbossweb.sar/server.xml```, add the following element below your Host element (default ```<Host name="localhost">```)

```xml
<Valve className="se.redpill.jboss.as5.CustomSingleSignOn" />
```
e.g, swap out the existing disabled... 
```xml
<!--
  <Valve className="org.jboss.web.tomcat.service.sso.SingleSignOn" />
-->
```
...with above

* Restart JBoss and make sure no error appear. All requests to the Host will now toggle through this filter

## Configure

### Web apps authenticating user sessions
If a non-standard (Basic/Digest/Form etc) authenticated web app would like to enable SSO for other web apps, it must after authentication pass the ```Principal``` Object as a request attribute. 

#### Seam example
In any Seam component

```java
@In
private Identity identity;

@Observer(Identity.EVENT_POST_AUTHENTICATE)
public void ssoPrep() {
	//Forward principal to CustomSingleSignOn (JBossWeb) valve
	((HttpServletRequest) facesContext.getExternalContext().getRequest()).setAttribute("principal", identity.getPrincipal());
}
```
```CustomSigneSignOn``` will pick up this Principal, cache it and create a SSO cookie so other web apps can use SSO

### Web apps using already authenticated user sessions
Basically nothing special, just enable standard security mechanisms.

Eg, in web.xml, add following section
```xml
<security-constraint>
	<web-resource-collection>
		<web-resource-name>All Access</web-resource-name>
		<url-pattern>/*</url-pattern>
	</web-resource-collection>
	<auth-constraint>
		<role-name>*</role-name>
	</auth-constraint>		
</security-constraint>
```
No ```<login-config>```, ```jboss-web.xml``` or ```security realms``` are needed - unless you want reauthenticate.

```CustomSigneSignOn``` will (just as in ```SingleSignOn```) pick up the SSO cookie, look up the Principal object and set request.setNote that will enable authenticting the new session.

## Details
The ```CustomSingleSignOn``` extends ```org.jboss.web.tomcat.service.sso.SingleSignOn```, so behaviour should be consistent except the extra action that happens if it finds a Principal object in Request. 
Again, heck out https://community.jboss.org/wiki/JBossWebSingleSignOn.

If needed, authenticating web app can besides "principal" also set following request attributes
* "username" - the username that may be used for reauthentication.  default is null
* "password" - the password that may be used for reauthentication default is null
* "authtype" - default is "NONE" (meaning the authentication mechanism used was non-standard)
* "sessionId" - the name of the session id cookie. Default is "JSESSIONID"

So features like "reauthentication" should (untested) work if needed.

## Issues
* Have not verified how this SSO cache evicts entries, but it should not differ from "standard" SingleSignOn handling. A little difference is that the custom web app cannot inform the valve that a user killed the session, but session expiration should not differ.
