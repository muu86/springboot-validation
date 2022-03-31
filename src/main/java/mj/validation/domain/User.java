package mj.validation.domain;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class User {
    
    @NotNull
    private Long id;
    
    @NotNull
    @Size(min = 1, message = "Email is required.")
    @Email(message = "Email is not well formmatted.")
    private String email;
    
    @NotNull(message = "Password is required,")
    @Size(min = 6, message = "Password should be at least 6 characters.")
    private String password;
    
    public User(Long id, String email, String password) {
        this.id = id;
        this.email = email;
        this.password = password;
    }

    public Long getId() { return id; }

    public String getEmail() {
        return email;
    }
    
    public String getPassword() {
        return password;
    }
}