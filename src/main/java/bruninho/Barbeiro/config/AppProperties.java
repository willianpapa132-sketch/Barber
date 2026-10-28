package bruninho.Barbeiro.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String zone = "America/Sao_Paulo";
    private boolean demoData;
    private final InitialAdmin initialAdmin = new InitialAdmin();
    private final MultiTenancy multiTenancy = new MultiTenancy();
    public MultiTenancy getMultiTenancy() { return multiTenancy; }

    public static class MultiTenancy {
        private boolean enabled;
        private String platformHost = "localhost";
        private String baseDomain = "localhost";
        private String encryptionKey = "";
        private String ownerLogin = "";
        private String ownerPassword = "";
        public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;}
        public String getPlatformHost(){return platformHost;} public void setPlatformHost(String v){platformHost=v;}
        public String getBaseDomain(){return baseDomain;} public void setBaseDomain(String v){baseDomain=v;}
        public String getEncryptionKey(){return encryptionKey;} public void setEncryptionKey(String v){encryptionKey=v;}
        public String getOwnerLogin(){return ownerLogin;} public void setOwnerLogin(String v){ownerLogin=v;}
        public String getOwnerPassword(){return ownerPassword;} public void setOwnerPassword(String v){ownerPassword=v;}
    }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
    public boolean isDemoData() { return demoData; }
    public void setDemoData(boolean demoData) { this.demoData = demoData; }
    public InitialAdmin getInitialAdmin() { return initialAdmin; }

    public static class InitialAdmin {
        private String login = "";
        private String password = "";
        private String name = "Administrador";
        public String getLogin() { return login; }
        public void setLogin(String login) { this.login = login; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
