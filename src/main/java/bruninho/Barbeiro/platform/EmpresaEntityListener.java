package bruninho.Barbeiro.platform;

import jakarta.persistence.*;
import org.hibernate.Hibernate;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Impede gravar vinculos entre empresas, inclusive em cascatas e entidades filhas. */
public class EmpresaEntityListener {
    @PrePersist @PreUpdate @PreRemove
    public void validate(Object entity) {
        TenantContext.requireEmpresaId();
        validate(entity, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private void validate(Object entity, Set<Object> visited) {
        if (entity == null || !visited.add(entity)) return;
        // Inicializa proxies de referencias para conferir a empresa real da entidade.
        entity = Hibernate.unproxy(entity);
        if (entity instanceof EmpresaScopedEntity scoped) scoped.bindEmpresa();
        for (Class<?> type = entity.getClass(); type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                boolean toOne = field.isAnnotationPresent(ManyToOne.class) || field.isAnnotationPresent(OneToOne.class);
                boolean toMany = field.isAnnotationPresent(ManyToMany.class) || field.isAnnotationPresent(OneToMany.class);
                if ((!toOne && !toMany) || field.getType() == Empresa.class) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    if (toOne) validate(value, visited);
                    else if (value instanceof Iterable<?> values && Hibernate.isInitialized(value)) {
                        for (Object item : values) validate(item, visited);
                    }
                } catch (IllegalAccessException e) { throw new IllegalStateException("Falha ao validar empresa", e); }
            }
        }
    }
}
