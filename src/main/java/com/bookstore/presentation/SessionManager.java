package com.bookstore.presentation;

import com.bookstore.domain.entity.User;

/**
 * Singleton для зберігання стану поточної сесії.
 *
 * <p>Зберігає авторизованого користувача між вікнами JavaFX. Патерн: Singleton.
 */
public class SessionManager {

    private static volatile SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    /**
     * Повертає єдиний екземпляр (Double-checked locking).
     *
     * @return єдиний екземпляр SessionManager
     */
    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }

    /**
     * Встановлює поточного користувача після входу.
     *
     * @param user авторизований користувач
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * Повертає поточного авторизованого користувача.
     *
     * @return поточний користувач або null якщо не авторизований
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Перевіряє чи є активна сесія.
     *
     * @return true якщо користувач авторизований
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /** Очищає сесію при виході. */
    public void logout() {
        this.currentUser = null;
    }
}