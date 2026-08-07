package com.fooddelivery.chat.dto;

import java.util.List;

public class TurnCredentialsResponse {
    private List<IceServer> iceServers;


    public static class IceServer {
        private String urls;
        private String username;
        private String credential;


        @java.lang.SuppressWarnings("all")
        public static class IceServerBuilder {
            @java.lang.SuppressWarnings("all")
            private String urls;
            @java.lang.SuppressWarnings("all")
            private String username;
            @java.lang.SuppressWarnings("all")
            private String credential;

            @java.lang.SuppressWarnings("all")
            IceServerBuilder() {
            }

            /**
             * @return {@code this}.
             */
            @java.lang.SuppressWarnings("all")
            public TurnCredentialsResponse.IceServer.IceServerBuilder urls(final String urls) {
                this.urls = urls;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @java.lang.SuppressWarnings("all")
            public TurnCredentialsResponse.IceServer.IceServerBuilder username(final String username) {
                this.username = username;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @java.lang.SuppressWarnings("all")
            public TurnCredentialsResponse.IceServer.IceServerBuilder credential(final String credential) {
                this.credential = credential;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            public TurnCredentialsResponse.IceServer build() {
                return new TurnCredentialsResponse.IceServer(this.urls, this.username, this.credential);
            }

            @java.lang.Override
            @java.lang.SuppressWarnings("all")
            public java.lang.String toString() {
                return "TurnCredentialsResponse.IceServer.IceServerBuilder(urls=" + this.urls + ", username=" + this.username + ", credential=" + this.credential + ")";
            }
        }

        @java.lang.SuppressWarnings("all")
        public static TurnCredentialsResponse.IceServer.IceServerBuilder builder() {
            return new TurnCredentialsResponse.IceServer.IceServerBuilder();
        }

        @java.lang.SuppressWarnings("all")
        public String getUrls() {
            return this.urls;
        }

        @java.lang.SuppressWarnings("all")
        public String getUsername() {
            return this.username;
        }

        @java.lang.SuppressWarnings("all")
        public String getCredential() {
            return this.credential;
        }

        @java.lang.SuppressWarnings("all")
        public void setUrls(final String urls) {
            this.urls = urls;
        }

        @java.lang.SuppressWarnings("all")
        public void setUsername(final String username) {
            this.username = username;
        }

        @java.lang.SuppressWarnings("all")
        public void setCredential(final String credential) {
            this.credential = credential;
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof TurnCredentialsResponse.IceServer)) return false;
            final TurnCredentialsResponse.IceServer other = (TurnCredentialsResponse.IceServer) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$urls = this.getUrls();
            final java.lang.Object other$urls = other.getUrls();
            if (this$urls == null ? other$urls != null : !this$urls.equals(other$urls)) return false;
            final java.lang.Object this$username = this.getUsername();
            final java.lang.Object other$username = other.getUsername();
            if (this$username == null ? other$username != null : !this$username.equals(other$username)) return false;
            final java.lang.Object this$credential = this.getCredential();
            final java.lang.Object other$credential = other.getCredential();
            if (this$credential == null ? other$credential != null : !this$credential.equals(other$credential)) return false;
            return true;
        }

        @java.lang.SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof TurnCredentialsResponse.IceServer;
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $urls = this.getUrls();
            result = result * PRIME + ($urls == null ? 43 : $urls.hashCode());
            final java.lang.Object $username = this.getUsername();
            result = result * PRIME + ($username == null ? 43 : $username.hashCode());
            final java.lang.Object $credential = this.getCredential();
            result = result * PRIME + ($credential == null ? 43 : $credential.hashCode());
            return result;
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "TurnCredentialsResponse.IceServer(urls=" + this.getUrls() + ", username=" + this.getUsername() + ", credential=" + this.getCredential() + ")";
        }

        @java.lang.SuppressWarnings("all")
        public IceServer() {
        }

        @java.lang.SuppressWarnings("all")
        public IceServer(final String urls, final String username, final String credential) {
            this.urls = urls;
            this.username = username;
            this.credential = credential;
        }
    }


    @java.lang.SuppressWarnings("all")
    public static class TurnCredentialsResponseBuilder {
        @java.lang.SuppressWarnings("all")
        private List<IceServer> iceServers;

        @java.lang.SuppressWarnings("all")
        TurnCredentialsResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TurnCredentialsResponse.TurnCredentialsResponseBuilder iceServers(final List<IceServer> iceServers) {
            this.iceServers = iceServers;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public TurnCredentialsResponse build() {
            return new TurnCredentialsResponse(this.iceServers);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "TurnCredentialsResponse.TurnCredentialsResponseBuilder(iceServers=" + this.iceServers + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static TurnCredentialsResponse.TurnCredentialsResponseBuilder builder() {
        return new TurnCredentialsResponse.TurnCredentialsResponseBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public List<IceServer> getIceServers() {
        return this.iceServers;
    }

    @java.lang.SuppressWarnings("all")
    public void setIceServers(final List<IceServer> iceServers) {
        this.iceServers = iceServers;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof TurnCredentialsResponse)) return false;
        final TurnCredentialsResponse other = (TurnCredentialsResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$iceServers = this.getIceServers();
        final java.lang.Object other$iceServers = other.getIceServers();
        if (this$iceServers == null ? other$iceServers != null : !this$iceServers.equals(other$iceServers)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof TurnCredentialsResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $iceServers = this.getIceServers();
        result = result * PRIME + ($iceServers == null ? 43 : $iceServers.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "TurnCredentialsResponse(iceServers=" + this.getIceServers() + ")";
    }

    @java.lang.SuppressWarnings("all")
    public TurnCredentialsResponse() {
    }

    @java.lang.SuppressWarnings("all")
    public TurnCredentialsResponse(final List<IceServer> iceServers) {
        this.iceServers = iceServers;
    }
}
