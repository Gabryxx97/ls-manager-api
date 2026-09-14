package it.lsimpresa.manager.workorder;

public interface IWorkOrderService {
    WorkOrderDto create(WorkOrderDto workOrder);
    WorkOrderDto update(Long id, WorkOrderDto workOrder);
    WorkOrderDto find(Long id);
    void delete(Long id);
}
