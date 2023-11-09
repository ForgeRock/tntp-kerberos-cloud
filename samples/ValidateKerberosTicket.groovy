/*
 * This code is to be used exclusively in connection with ForgeRock’s software or services. 
 * ForgeRock only offers ForgeRock software or services to legal entities who have entered 
 * into a binding license agreement with ForgeRock.  
 */

package IdentityGateway

import org.forgerock.util.encode.Base64;

import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.kerberos.KerberosPrincipal
import javax.security.auth.login.LoginContext;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.forgerock.http.protocol.Response;
import java.util.Properties;

def properties = new Properties();
def groovyProp = System.getenv("IG_PropFile");
properties.load(new FileInputStream(groovyProp));
def spnegoPath =  System.getenv("spnego_conf");

response = new Response(Status.OK);

int i = 0;
if (request.headers!=null && request.headers.authorization!=null && request.headers.authorization.values[0]!==null && request.headers.authorization.values[0].startsWith("Negotiate")){
  try {
        logger.warn("Authorization Header: " + request.headers.authorization.values[0]);
        def theToken = request.headers.authorization.values[0].substring(10);
        def decodedToken = Base64.decode(theToken);
        String output = new String(decodedToken);

        if (isNtlmMechanism(decodedToken)) {
          output = "Got request for unsupported NTLM mechanism, aborting negotiation.";
          //TODO - continue without requesting creds.  We do not want to support NTLM
        }

        String domainUsername = properties.adServiceUser;
        String domainUserPassword = properties.adServiceUserCredential;
        CallbackHandler handler = getUsernamePasswordHandler(domainUsername, domainUserPassword);
        LoginContext loginContext = null;


        System.properties['java.security.auth.login.config'] = spnegoPath;
        System.properties['javax.security.auth.useSubjectCredsOnly'] = true;
        System.properties['sun.security.spnego.msinterop'] = true; // true by default
        System.properties['sun.security.spnego.debug'] = false;// false by default

        loginContext = new LoginContext("spnego-server", handler);
        loginContext.login();
        Subject subject = loginContext.getSubject();
        logger.warn("Subject used: " + subject.toString());


        Oid spnegoOid = new Oid("1.3.6.1.5.5.2"); // for spnego answers
        Oid kerbv5Oid = new Oid("1.2.840.113554.1.2.2"); // for chromium (they send a kerbv5 token instead of spnego)
        def oids = [spnegoOid, kerbv5Oid] as Oid[];

        GSSManager manager = GSSManager.getInstance();
        PrivilegedExceptionAction<GSSCredential> action = new PrivilegedExceptionAction<GSSCredential>() {
          public GSSCredential run() throws GSSException {
                return manager.createCredential(null, GSSCredential.INDEFINITE_LIFETIME, oids, GSSCredential.ACCEPT_ONLY);
          }
        };

          GSSCredential serverCreds = Subject.doAs(subject, action);

        logger.warn("Mechs: " + Arrays.toString(serverCreds.getMechs()));

        GSSContext gssContext = null;
        gssContext = manager.createContext(serverCreds);

        logger.warn("Context created. " + gssContext);

        def tokenBytes = gssContext.acceptSecContext(decodedToken, 0, decodedToken.length);
        outToken = Base64.encode(tokenBytes);


        Subject subject2 = new Subject();
        GSSName clientGSSName = gssContext.getSrcName();
        KerberosPrincipal clientPrincipal = new KerberosPrincipal(clientGSSName.toString());
        subject2.getPrincipals().add(clientPrincipal);
        String uln = clientPrincipal.getName();
        String theRealm = clientPrincipal.getRealm();
        String samaccountname = uln.substring(0,uln.indexOf(theRealm)-1);
        attributes.kerbUsername = samaccountname;
        attributes.Server = theRealm;
        logger.warn("Hello, " + samaccountname.toUpperCase() + " from " + theRealm + ".  You are headed to the cloud!");
        return next.handle(context, request).thenOnResult { response ->logger.info('We are done in Kerb Checking')};
  }
  catch(Exception e) {
        response = new Response(Status.INTERNAL_SERVER_ERROR)
        response.headers['Content-Type'] = "text/html; charset=utf-8"
        response.entity = "<html><p>Server error: " + e.message + " error postion: " + i + "</p></html>"
        logger.warn("Here is the error!!: " + e.toString());
  }
}
else{
  response.status = Status.UNAUTHORIZED;
  response.headers['WWW-Authenticate'] = "Negotiate";
}


static boolean isNtlmMechanism(byte[] gssapiData) {
  byte[] NTLMSSP = [
        (byte) 0x4E,
        (byte) 0x54,
        (byte) 0x4C,
        (byte) 0x4D,
        (byte) 0x53,
        (byte) 0x53,
        (byte) 0x50
  ];
  def leadingBytes = new byte[7];
  System.arraycopy(gssapiData, 0, leadingBytes, 0, 7);
  if (Arrays.equals(leadingBytes, NTLMSSP)) {
        return true;
  }

  return false;
}

static CallbackHandler getUsernamePasswordHandler(String uid, String pwd) {

  final CallbackHandler handler = new CallbackHandler() {
        public void handle(final Callback[] callback) {
          for (int i=0; i<callback.length; i++) {
                if (callback[i] instanceof NameCallback) {
                  final NameCallback nameCallback = (NameCallback) callback[i];
                  nameCallback.setName(uid);
                } else if (callback[i] instanceof PasswordCallback) {
                  final PasswordCallback passCallback = (PasswordCallback) callback[i];
                  passCallback.setPassword(pwd.toCharArray());
                } else {
                  //do nothing

                }
          }
        }
  };
        return handler;
}

// Return the locally created response, no need to wrap it into a Promise
return response
