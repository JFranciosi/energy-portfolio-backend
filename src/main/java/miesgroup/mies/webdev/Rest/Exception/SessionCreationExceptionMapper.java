package miesgroup.mies.webdev.Rest.Exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import miesgroup.mies.webdev.Service.exception.SessionCreationException;

@Provider
public class SessionCreationExceptionMapper implements ExceptionMapper<SessionCreationException> {

    @Override
    public Response toResponse(SessionCreationException exception) {
        return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(exception.getMessage())
                .build();
    }
}