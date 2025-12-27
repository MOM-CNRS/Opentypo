package fr.cnrs.opentypo.bean.users;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
    
    public enum Role {
        ADMIN,
        EDITOR,
        VIEWER
    }
    
    private Long id;
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private Role role = Role.VIEWER;
    private boolean active = true;
    private LocalDateTime dateCreation = LocalDateTime.now();
    private LocalDateTime dateModification;
    private String createdBy;
}

