package com.fooddelivery.chat.dto;

import java.util.List;@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.Data
@lombok.Builder


public class TurnCredentialsResponse {
    @jakarta.validation.constraints.NotNull
    private List<IceServer> iceServers;


    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class IceServer {
        @jakarta.validation.constraints.NotNull
        private String urls;
        private String username;
        private String credential;


}
}
