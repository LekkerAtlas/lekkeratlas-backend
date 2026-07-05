package nl.lekkeratlas.backendapi.exceptions;

import org.springframework.http.HttpStatus;

import nl.lekkeratlas.shared.exceptions.HttpResponsableException;

public class JsonWebTokenException extends HttpResponsableException {

        public JsonWebTokenException(HttpStatus status, String message) {
                super(status, message);
        }
}
