package bruninho.Barber.repository;

import bruninho.Barber.domain.ServiceCatalog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalog, Long> {
    List<ServiceCatalog> findByActiveTrueOrderByNome();
    List<ServiceCatalog> findAllByOrderByNome();
}
