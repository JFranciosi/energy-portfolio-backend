package miesgroup.mies.webdev.Service.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ClienteCreationExceptionMapper implements ExceptionMapper<ClienteCreationException> {
    @Override
    public Response toResponse(ClienteCreationException exception){
        return Response.status(Response.Status.BAD_REQUEST).entity(exception.getMessage()).build();
    }
}
