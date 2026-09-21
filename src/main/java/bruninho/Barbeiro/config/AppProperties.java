package bruninho.Barbeiro.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String zone = "America/Sao_Paulo";
    private boolean demoData;
    private final InitialAdmin initialAdmin = new InitialAdmin();

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
