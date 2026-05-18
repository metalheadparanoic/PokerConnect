package poker.common.messages;

/**
 * Error message sent to client when an action fails.
 */
public class ErrorMessage {
    private String errorCode; // INVALID_ACTION, INSUFFICIENT_CHIPS, NOT_YOUR_TURN, etc.
    private String message;
    private String details;
    
    public ErrorMessage() {
    }
    
    public ErrorMessage(String errorCode, String message, String details) {
        this.errorCode = errorCode;
        this.message = message;
        this.details = details;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
}
