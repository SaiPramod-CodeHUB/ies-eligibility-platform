package com.sgopireddy.ies.notices;

public class NoticeNotFoundException extends RuntimeException {
    public NoticeNotFoundException(Long determinationId) {
        super("No notice has been generated for determination " + determinationId);
    }
}
