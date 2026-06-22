package nl.lekkeratlas.backendapi;

import java.sql.SQLException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import nl.lekkeratlas.shared.exceptions.HttpResponsableException;

@ControllerAdvice
public class GlobalExceptionHandler {
        public static final String MESSAGE_KEY = "message";

        private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        private static final ResponseEntity<Map<String, String>> internalServerErrorDefualtResponse = ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(MESSAGE_KEY, "An unexpected internal error occurred."));

        @ExceptionHandler(SQLException.class)
        public ResponseEntity<Map<String, String>> handleSqlException(SQLException ex) {
                // Log the internal error for debugging purposes
                logger.error("Uncatched SQLException", ex);

                // Hide details from the client
                return internalServerErrorDefualtResponse;
        }

        @ExceptionHandler(NoSuchFieldException.class)
        public ResponseEntity<Map<String, String>> handleNoSuchFieldException(NoSuchFieldException ex) {
                // Log the internal error for debugging purposes
                logger.error("Uncatched reflection error", ex);

                // Hide details from the client
                return internalServerErrorDefualtResponse;

        }

        @ExceptionHandler(HttpResponsableException.class)
        public ResponseEntity<Map<String, String>> handleJsonErrorResponseException(HttpResponsableException ex) {
                return ex.getResponse();
        }
}
