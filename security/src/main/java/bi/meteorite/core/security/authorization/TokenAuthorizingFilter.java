/*
 * Copyright 2015 OSBI Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bi.meteorite.core.security.authorization;

import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;

/**
 * Token Authorizing Filter.
 */
@Priority(Priorities.AUTHORIZATION)
@Singleton
@Named("authorizationFilter")
public class TokenAuthorizingFilter implements ContainerRequestFilter {

  @Inject
  @Named("authorizationInterceptor")
  private TokenAuthorizingInterceptor interceptor;

  public void filter(ContainerRequestContext context) {
    try {
      interceptor.handleMessage(JAXRSUtils.getCurrentMessage());
    } catch (AccessDeniedException ex) {
      context.abortWith(Response.status(Response.Status.FORBIDDEN).build());
    }
  }

  public void setInterceptor(TokenAuthorizingInterceptor in) {
    interceptor = in;
  }
}
