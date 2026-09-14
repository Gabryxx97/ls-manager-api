package it.lsimpresa.manager.workorder;

import it.lsimpresa.manager.utils.DtoMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WorkOrderMapper implements DtoMapper<WorkOrderDto, WorkOrderJpa> {
    public WorkOrderDto toDto(WorkOrderJpa entity) {
        if (entity == null) return null;
        WorkOrderDto dto = new WorkOrderDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        return dto;
    }

    @Override
    public List<WorkOrderDto> map(List<WorkOrderJpa> entities) {
        return entities.stream().map(this::toDto).toList();
    }
}
