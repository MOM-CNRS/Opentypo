package fr.cnrs.opentypo.bean;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Data;

import java.io.Serializable;

@Data
@Named(value = "userBean")
@SessionScoped
public class UserBean implements Serializable {
    private String username;
}
