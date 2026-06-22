package nl.lekkeratlas.shared.exceptions;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import nl.lekkeratlas.backendapi.GlobalExceptionHandler;

public abstract class HttpResponsableException extends Exception {

        private final HttpStatus httpStatus;
        private final String message;

        protected HttpResponsableException(HttpStatus httpStatus, String message) {
                this.httpStatus = httpStatus;
                this.message = message;
        }

        public ResponseEntity<Map<String, String>> getResponse() {

                return ResponseEntity
                                .status(httpStatus)
                                .body(Map.of(GlobalExceptionHandler.MESSAGE_KEY,
                                                message));
        }

}
