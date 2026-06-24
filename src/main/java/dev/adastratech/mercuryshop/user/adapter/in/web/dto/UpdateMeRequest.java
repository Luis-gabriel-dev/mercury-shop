package dev.adastratech.mercuryshop.user.adapter.in.web.dto;

import jakarta.validation.constraints.Size;

/** PATCH parcial: campos nulos não são alterados. */
public record UpdateMeRequest(

        @Size(max = 160)
        String fullName,

        @Size(max = 40)
        String phone) {
}
