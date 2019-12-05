package net.trystram.scaletest.infinispan;

import java.util.Base64;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/rest/v2")
@RegisterRestClient
@ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
public interface Infinispan {

    @GET
    @Path("/caches/{name}?action=size")
    @Produces("text/plain")
    public long cacheSize(@PathParam String name);

    default String lookupAuth() {
        final String credentials = ConfigProvider.getConfig().getValue("infinispan.credentials", String.class);
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
      }
}
