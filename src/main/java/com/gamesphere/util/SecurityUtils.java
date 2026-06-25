package com.gamesphere.util;

import com.gamesphere.security.UserPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static UserPrincipal getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal) {
            return (UserPrincipal) principal;
        }
        throw new IllegalStateException("User is not authenticated");
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
