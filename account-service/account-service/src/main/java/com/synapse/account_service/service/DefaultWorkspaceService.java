package com.synapse.account_service.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.synapse.account_service.domain.entity.Member;

@Service
public class DefaultWorkspaceService {

    public UUID createDefaultWorkspaceId(UUID memberId) {
        // TODO: Replace with a call to the Workspace-Service (REST/gRPC) that
        //       creates a workspace and returns the generated identifier.
        return memberId;
    }

    public UUID getDefaultWorkspaceId(Member member) {
        UUID workspaceId = member.getDefaultWorkspaceId();
        if (workspaceId == null) {
            throw new IllegalStateException("Default workspace is not initialized for member " + member.getId());
        }
        return workspaceId;
    }
}
