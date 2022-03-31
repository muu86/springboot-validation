package mj.validation.controller.result;

public class Success {
    
    private String message;
    
    public Success(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}