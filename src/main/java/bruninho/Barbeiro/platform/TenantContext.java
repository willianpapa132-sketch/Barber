package bruninho.Barbeiro.platform;

public final class TenantContext {
    public static final String PLATFORM = "PLATFORM";
    public static final String SESSION_SCOPE = "empresaScope";
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();
    private TenantContext(){}
    public static String get(){return CURRENT.get();}
    public static void set(String id){CURRENT.set(id);}
    public static void clear(){CURRENT.remove();}
}
