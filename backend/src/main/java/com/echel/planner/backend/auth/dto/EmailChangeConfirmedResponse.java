package com.echel.planner.backend.auth.dto;

/** Returned after an email change is confirmed, echoing the address now in effect. */
public record EmailChangeConfirmedResponse(String email) {
}
