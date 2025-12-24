package fr.cnrs.opentypo.bean;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

@Named("notificationBean")
@RequestScoped
public class NotificationBean {

    public void showAlerts() {
        // placeholder for future notifications
    }
}

