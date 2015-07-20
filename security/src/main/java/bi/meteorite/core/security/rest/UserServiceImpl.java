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

package bi.meteorite.core.security.rest;

import bi.meteorite.core.api.objects.MeteoriteUser;
import bi.meteorite.core.api.security.MeteoriteSecurityException;
import bi.meteorite.core.api.security.rest.UserService;

import org.ops4j.pax.cdi.api.OsgiServiceProvider;

import javax.inject.Singleton;
import javax.ws.rs.core.Response;

/**
 * Created by bugg on 10/07/15.
 */
@OsgiServiceProvider(classes = { UserService.class })
@Singleton
public class UserServiceImpl implements UserService {
  @Override
  public Response addUser(MeteoriteUser u) throws MeteoriteSecurityException {
    return null;
  }

  @Override
  public Response modifyUser(MeteoriteUser u) throws MeteoriteSecurityException {
    return null;
  }

  @Override
  public Response deleteUser(MeteoriteUser u) throws MeteoriteSecurityException {
    return null;
  }

  @Override
  public Response deleteUser(int id) throws MeteoriteSecurityException {
    return null;
  }

  @Override
  public Response addGroup(String group) throws MeteoriteSecurityException {
    return null;
  }

  @Override
  public Response deleteGroup(int id) throws MeteoriteSecurityException {
    return null;
  }

  @Override
  public Response getExistingUsers() throws MeteoriteSecurityException {
    return Response.ok().entity("{hello}").build();
  }

  @Override
  public Response getUser(int id) throws MeteoriteSecurityException {
    return null;
  }
}