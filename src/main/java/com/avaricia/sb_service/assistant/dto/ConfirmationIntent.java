package com.avaricia.sb_service.assistant.dto;

/**
 * Represents the user's intent when responding to a confirmation request.
 */
public enum ConfirmationIntent {
    /**
     * User confirms the pending action
     */
    CONFIRM,
    
    /**
     * User cancels/rejects the pending action
     */
    CANCEL,
    
    /**
     * User's intent is unclear - neither confirmation nor cancellation
     * The message should be processed as a new request
     */
    UNCLEAR
}
