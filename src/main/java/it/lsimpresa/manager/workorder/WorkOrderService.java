package it.lsimpresa.manager.workorder;

import it.lsimpresa.manager.exception.ConflictException;
import it.lsimpresa.manager.exception.ResourceNotFoundException;
import it.lsimpresa.manager.order.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class WorkOrderService implements IWorkOrderService {
    private final WorkOrderRepository repository;
    private final OrderRepository orderRepository;
    private final WorkOrderMapper mapper;

    @Override
    @Transactional
    public WorkOrderDto create(WorkOrderDto source) {
        WorkOrderJpa entity = new WorkOrderJpa();
        apply(source, entity);
        ensureUnique(entity.getNameKey(), null);
        try {
            return mapper.toDto(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw duplicateName();
        }
    }

    @Override
    @Transactional
    public WorkOrderDto update(Long id, WorkOrderDto source) {
        WorkOrderJpa entity = findEntity(id);
        apply(source, entity);
        ensureUnique(entity.getNameKey(), id);
        try {
            repository.saveAndFlush(entity);
            orderRepository.synchronizeWorkOrderName(id, entity.getName());
            return mapper.toDto(entity);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateName();
        }
    }

    @Override
    public WorkOrderDto find(Long id) {
        return mapper.toDto(findEntity(id));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        WorkOrderJpa entity = findEntity(id);
        if (orderRepository.existsByWorkOrderId(id)) {
            throw new ConflictException("La commessa è utilizzata da uno o più ordini");
        }
        try {
            repository.delete(entity);
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("La commessa è utilizzata da uno o più ordini");
        }
    }

    public WorkOrderJpa findEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commessa non trovata"));
    }

    private void apply(WorkOrderDto source, WorkOrderJpa target) {
        String name = WorkOrderJpa.normalizeName(source.getName());
        if (name.isBlank()) throw new IllegalArgumentException("Il nome è obbligatorio");
        if (name.length() > 255) {
            throw new IllegalArgumentException("Il nome non può superare 255 caratteri");
        }
        target.setName(name);
        target.setNameKey(name.toLowerCase(Locale.ROOT));
        target.setDescription(normalizeOptional(source.getDescription()));
    }

    private void ensureUnique(String nameKey, Long excludedId) {
        boolean duplicate = excludedId == null
                ? repository.existsByNameKey(nameKey)
                : repository.existsByNameKeyAndIdNot(nameKey, excludedId);
        if (duplicate) throw duplicateName();
    }

    private ConflictException duplicateName() {
        return new ConflictException("Esiste già una commessa con questo nome");
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
