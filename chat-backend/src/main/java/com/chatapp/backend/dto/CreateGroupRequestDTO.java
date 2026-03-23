package com.chatapp.backend.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public class CreateGroupRequestDTO {

    @NotBlank(message = "Group title is required")
    @Size(min = 1, max = 100, message = "Title must be between 1 and 100 characters")
    private String title;

    @NotEmpty(message = "At least one member is required")
    private List<Integer> memberIds;

    public CreateGroupRequestDTO() {
    }

    public CreateGroupRequestDTO(String title, List<Integer> memberIds) {
        this.title = title;
        this.memberIds = memberIds;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Integer> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<Integer> memberIds) {
        this.memberIds = memberIds;
    }
}
