package fr.cnrs.opentypo.conf;

import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerFactory;

/**
 * Factory pour créer un gestionnaire d'exceptions personnalisé
 * qui gère toutes les exceptions (ViewExpiredException et autres erreurs techniques)
 */
public class ViewExpiredExceptionHandlerFactory extends ExceptionHandlerFactory {

    private final ExceptionHandlerFactory parent;

    public ViewExpiredExceptionHandlerFactory(ExceptionHandlerFactory parent) {
        this.parent = parent;
    }

    @Override
    public ExceptionHandler getExceptionHandler() {
        // Envelopper le gestionnaire ViewExpiredException dans le gestionnaire global
        ExceptionHandler viewExpiredHandler = new ViewExpiredExceptionHandler(parent.getExceptionHandler());
        return new GlobalExceptionHandler(viewExpiredHandler);
    }
}

