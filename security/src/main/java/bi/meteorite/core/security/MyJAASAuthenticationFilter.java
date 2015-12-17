package bi.meteorite.core.security;


import bi.meteorite.core.api.security.AdminLoginService;
import bi.meteorite.core.api.security.exceptions.TokenProviderException;
import bi.meteorite.core.api.security.tokenprovider.TokenProvider;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.security.JAASLoginInterceptor;
import org.apache.cxf.interceptor.security.NamePasswordCallbackHandler;
import org.apache.cxf.interceptor.security.callback.CallbackHandlerProvider;
import org.apache.cxf.interceptor.security.callback.CallbackHandlerProviderAuthPol;
import org.apache.cxf.interceptor.security.callback.CallbackHandlerProviderUsernameToken;
import org.apache.cxf.jaxrs.impl.HttpHeadersImpl;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;

import java.net.URI;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.Priority;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.login.Configuration;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;

/**
 * Created by bugg on 16/12/15.
 */
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class MyJAASAuthenticationFilter implements ContainerRequestFilter {

  private volatile AdminLoginService adminLoginService;

  private static final List<MediaType> HTML_MEDIA_TYPES =
      Arrays.asList(MediaType.APPLICATION_XHTML_XML_TYPE, MediaType.TEXT_HTML_TYPE);
  private final ArrayList<CallbackHandlerProvider> callbackHandlerProviders;

  private URI redirectURI;
  private String realmName;
  private boolean ignoreBasePath = true;

  private JAASLoginInterceptor interceptor;
  private TokenProvider tokenProvider;

  public MyJAASAuthenticationFilter() {
    this.callbackHandlerProviders = new ArrayList<CallbackHandlerProvider>();
    this.callbackHandlerProviders.add(new CallbackHandlerProviderAuthPol());
    this.callbackHandlerProviders.add(new CallbackHandlerProviderUsernameToken());
    interceptor = new JAASLoginInterceptor() {
      protected CallbackHandler getCallbackHandler(String name, String password) {
        return MyJAASAuthenticationFilter.this.getCallbackHandler(name, password);
      }
    };
    interceptor.setUseDoAs(false);
  }

  @Deprecated
  public void setRolePrefix(String name) {
    interceptor.setRolePrefix(name);
  }


  public void setIgnoreBasePath(boolean ignore) {
    this.ignoreBasePath = ignore;
  }

  public void setContextName(String name) {
    interceptor.setContextName(name);
  }

  public void setLoginConfig(Configuration config) {
    interceptor.setLoginConfig(config);
  }

  public void setRoleClassifier(String rc) {
    interceptor.setRoleClassifier(rc);
  }

  public void setRoleClassifierType(String rct) {
    interceptor.setRoleClassifierType(rct);
  }


  public void setRedirectURI(String uri) {
    this.redirectURI = URI.create(uri);
  }

  public void setRealmName(String name) {
    this.realmName = name;
  }

  protected CallbackHandler getCallbackHandler(String name, String password) {
    return new NamePasswordCallbackHandler(name, password);
  }


  public void filter(ContainerRequestContext context) {
    Message m = JAXRSUtils.getCurrentMessage();
    try {
      SortedMap<String, String> valid = null;
      Map<String, Cookie> cookies = context.getCookies();
      if(cookies.containsKey(TokenProvider.TOKEN_COOKIE_NAME)){
        try {
          Cookie cookie = cookies.get(TokenProvider.TOKEN_COOKIE_NAME);
          valid = tokenProvider.verifyToken(cookie.getValue());
          final SortedMap<String, String> finalValid = valid;
          SecurityContext c = new SecurityContext() {
            java.security.Principal p = new UserPrincipal(finalValid.get(TokenProvider.USERNAME));

            @Override
            public Principal getUserPrincipal() {
              return p;
            }

            @Override
            public boolean isUserInRole(String role) {
              String roles = finalValid.get(TokenProvider.ROLES);
              String[] rolearray = roles.split(",");
              return Arrays.asList(rolearray).contains(role);
            }

            @Override
            public boolean isSecure() {
              return false;
            }

            @Override
            public String getAuthenticationScheme() {
              return null;
            }
          };

          context.setSecurityContext(c);
        } catch (TokenProviderException e) {
          e.printStackTrace();
        }
      }
      if(valid == null || valid.size()==0) {

        CallbackHandler handler = getFirstCallbackHandler(m);


        interceptor.handleMessage(m);


        AccessControlContext acc = AccessController.getContext();
        Subject subject = Subject.getSubject(acc);
        Set<Principal> principals = subject.getPrincipals();

        String s = "";
        for(Principal role:principals){
          if(role instanceof RolePrincipal){
            s+=role.getName()+",";
          }

        }
        s = s.substring(0, s.length()-1);

        SortedMap<String, String> userMap = new TreeMap<>();
        userMap.put(TokenProvider.USERNAME, getUsername(handler));
        userMap.put(TokenProvider.ROLES, s);
        try {
          String token = tokenProvider.generateToken(userMap);
          context.setProperty("test", token);
        } catch (TokenProviderException e) {
          e.printStackTrace();
        }
      }

    } catch (SecurityException ex) {
      context.abortWith(handleAuthenticationException(ex, m));
    }


  }

  private String getUsername(CallbackHandler handler) {
    if (handler == null) {
      return null;
    }
    try {
      NameCallback usernameCallBack = new NameCallback("user");
      handler.handle(new Callback[]{usernameCallBack });
      return usernameCallBack.getName();
    } catch (Exception e) {
      return null;
    }
  }

  private CallbackHandler getFirstCallbackHandler(Message message) {
    for (CallbackHandlerProvider cbp : callbackHandlerProviders) {
      CallbackHandler cbh = cbp.create(message);
      if (cbh != null) {
        return cbh;
      }
    }
    return null;
  }

  public void setTokenProvider(TokenProvider tokenProvider) {
    this.tokenProvider = tokenProvider;
  }

  protected Response handleAuthenticationException(SecurityException ex, Message m) {
    HttpHeaders headers = new HttpHeadersImpl(m);
    if (redirectURI != null && isRedirectPossible(headers)) {

      URI finalRedirectURI = null;

      if (!redirectURI.isAbsolute()) {
        String endpointAddress = HttpUtils.getEndpointAddress(m);
        Object basePathProperty = m.get(Message.BASE_PATH);
        if (ignoreBasePath && basePathProperty != null && !"/".equals(basePathProperty)) {
          int index = endpointAddress.lastIndexOf(basePathProperty.toString());
          if (index != -1) {
            endpointAddress = endpointAddress.substring(0, index);
          }
        }
        finalRedirectURI = UriBuilder.fromUri(endpointAddress).path(redirectURI.toString()).build();
      } else {
        finalRedirectURI = redirectURI;
      }

      return Response.status(getRedirectStatus()).
          header(HttpHeaders.LOCATION, finalRedirectURI).build();
    } else {
      Response.ResponseBuilder builder = Response.status(Response.Status.UNAUTHORIZED);

      StringBuilder sb = new StringBuilder();

      List<String> authHeader = headers.getRequestHeader(HttpHeaders.AUTHORIZATION);
      if (authHeader != null && authHeader.size() > 0) {
        // should HttpHeadersImpl do it ?
        String[] authValues = StringUtils.split(authHeader.get(0), " ");
        if (authValues.length > 0) {
          sb.append(authValues[0]);
        }
      } else {
        sb.append("Basic");
      }
      if (realmName != null) {
        sb.append(" realm=\"").append(realmName).append('"');
      }
      builder.header(HttpHeaders.WWW_AUTHENTICATE, sb.toString());

      return builder.build();
    }
  }

  protected Response.Status getRedirectStatus() {
    return Response.Status.TEMPORARY_REDIRECT;
  }

  protected boolean isRedirectPossible(HttpHeaders headers) {
    List<MediaType> clientTypes = headers.getAcceptableMediaTypes();
    return !JAXRSUtils.intersectMimeTypes(clientTypes, HTML_MEDIA_TYPES, false)
                      .isEmpty();
  }

  public class UserPrincipal implements Principal {

    private String name;

    @Override
    public String getName() {
      return name;
    }

    public UserPrincipal(String name) {
      this.name = name;
    }

  }

  public void setAdminLoginService(AdminLoginService adminLoginService) {
    this.adminLoginService = adminLoginService;
  }

}
