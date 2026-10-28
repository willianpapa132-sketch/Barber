package bruninho.Barbeiro.platform;

import bruninho.Barbeiro.config.AppProperties;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public final class TenantSecrets {
    private final byte[] key;
    private final SecureRandom random=new SecureRandom();
    public TenantSecrets(AppProperties props){
        try { key=Base64.getDecoder().decode(props.getMultiTenancy().getEncryptionKey()); }
        catch(IllegalArgumentException e){throw new IllegalStateException("Configure APP_TENANT_ENCRYPTION_KEY em Base64");}
        if(key.length!=32)throw new IllegalStateException("APP_TENANT_ENCRYPTION_KEY deve conter 32 bytes em Base64");
    }
    public String encrypt(String value){
        try{
            byte[] iv=new byte[12];random.nextBytes(iv);
            var c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,iv));
            return Base64.getEncoder().encodeToString(iv)+":"+Base64.getEncoder().encodeToString(c.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        }catch(Exception e){throw new IllegalStateException("Nao foi possivel proteger a credencial do banco",e);}
    }
    public String decrypt(String value){
        try{
            var parts=value.split(":",2);var c=Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,Base64.getDecoder().decode(parts[0])));
            return new String(c.doFinal(Base64.getDecoder().decode(parts[1])),StandardCharsets.UTF_8);
        }catch(Exception e){throw new IllegalStateException("Nao foi possivel abrir a credencial do banco",e);}
    }
}
