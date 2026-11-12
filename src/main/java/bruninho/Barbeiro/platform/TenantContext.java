package bruninho.Barbeiro.platform;

public final class TenantContext {
    public static final String PLATFORM = "PLATFORM";
    public static final String SESSION_SCOPE = "empresaScope";
    private static final ThreadLocal<java.util.UUID> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> PLATFORM_REQUEST = new ThreadLocal<>();
    private TenantContext(){}
    public static java.util.UUID getEmpresaId(){return CURRENT.get();}
    public static java.util.UUID requireEmpresaId(){
        var id = CURRENT.get();
        if (id == null) throw new IllegalStateException("Operacao da barbearia sem empresa identificada");
        return id;
    }
    public static void setEmpresaId(java.util.UUID id){
        if (id == null) throw new IllegalArgumentException("Empresa obrigatoria");
        PLATFORM_REQUEST.remove();
        CURRENT.set(id);
    }
    public static void setPlatform(){CURRENT.remove(); PLATFORM_REQUEST.set(true);}
    public static boolean isPlatform(){return Boolean.TRUE.equals(PLATFORM_REQUEST.get());}
    public static String sessionScope(){return isPlatform() ? PLATFORM : requireEmpresaId().toString();}
    public static void clear(){CURRENT.remove(); PLATFORM_REQUEST.remove();}
}
