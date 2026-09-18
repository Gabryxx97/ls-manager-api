package it.lsimpresa.manager.utils.search;

import it.lsimpresa.manager.utils.DtoMapper;
import it.lsimpresa.manager.utils.search.operators.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Service
public class SearchService implements ISearchService{

	@PersistenceContext
    private EntityManager entityManager;

	@Override
	@Transactional
	public <R, T> Page<R> search(
			Map<String, Operator> filters,
			Map<String, Direction> order,
			Pageable pageable,
			Class<T> clazz,
			DtoMapper<R, T> mapper) {

		// Query building
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<T> cq = cb.createQuery(clazz);
		Root<T> entity = cq.from(clazz);
		List<Predicate> predicates = createCriteria(cb, entity, filters);
		cq.select(entity).where(predicates.toArray(new Predicate[0]));

		// Count
		CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
		Root<T> entityCount = countQuery.from(clazz);
		countQuery.select(cb.count(entityCount));
		List<Predicate> predicatesCount = createCriteria(cb, entityCount, filters);
		countQuery.where(predicatesCount.toArray(new Predicate[0]));
		Long count = entityManager.createQuery(countQuery).getSingleResult();

		// Sort
		List<Order> orders = new ArrayList<>();
		for (Entry<String, Direction> entry : order.entrySet()) {
			if (entry.getValue().isAscending()) {
				orders.add(cb.asc(entity.get(entry.getKey())));
			} else {
				orders.add(cb.desc(entity.get(entry.getKey())));
			}
		}
		cq.orderBy(orders);

		TypedQuery<T> query = entityManager.createQuery(cq);
		query.setFirstResult(pageable.getPageNumber() * pageable.getPageSize());
		query.setMaxResults(pageable.getPageSize());

		return PageableExecutionUtils.getPage(mapper.map(query.getResultList()), pageable, () -> count);
	}

	private List<Predicate> createCriteria(
			CriteriaBuilder cb,
			Root<?> entity,
			Map<String, Operator> filters) {
		if (filters == null || filters.isEmpty()) return new ArrayList<>();
		List<Predicate> res = new ArrayList<>();

		if (filters.keySet().size() == 1) {
			Operator op = filters.entrySet().iterator().next().getValue();
			String field = filters.keySet().iterator().next();

			// Qui controlliamo se il filtro riguarda un campo derivato da una relazione.
			if ("userId".equals(field)) {
				res.add(cb.equal(entity.get("user").get("id"), filters.get(field).getValues()[0]));
			} else if ("createdById".equals(field)) {
				res.add(cb.equal(entity.get("createdBy").get("id"), filters.get(field).getValues()[0]));
			} else if ("orderId".equals(field)) {
				res.add(cb.equal(entity.get("order").get("id"), filters.get(field).getValues()[0]));
			} else {
				res.add(this.criteria(cb, entity, field, op));
			}
		} else {
			for (String field : filters.keySet()) {
				if ("userId".equals(field)) {
					res.add(cb.equal(entity.get("user").get("id"), filters.get(field).getValues()[0]));
				} else if ("createdById".equals(field)) {
					res.add(cb.equal(entity.get("createdBy").get("id"), filters.get(field).getValues()[0]));
				} else if ("orderId".equals(field)) {
					res.add(cb.equal(entity.get("order").get("id"), filters.get(field).getValues()[0]));
				} else {
					Operator op = filters.get(field);
					res.add(this.criteria(cb, entity, field, op));
				}
			}
		}
		return res;
	}

	private Predicate criteria(
			CriteriaBuilder cb,
			Root<?> entity ,
			String field, 
			Operator op) {
		Predicate res=null;
		Object[] values = convertValues(entity.get(field).getJavaType(), op.getValues());
		switch (op.getOperator()) {
		case Equal.OPERATOR:
			Equal eq=new Equal(cb,entity);
			res=eq.getCriteria(field, values);
			break;
		case EqualOrNull.OPERATOR:
			EqualOrNull eqOrNull = new EqualOrNull(cb, entity);
			res = eqOrNull.getCriteria(field, values);
			break;
		case Between.OPERATOR:
			Between bet=new Between(cb,entity);
			res=bet.getCriteria(field, values);
			break;
		case Like.OPERATOR:
			Like lik=new Like(cb,entity);
			res=lik.getCriteria(field, values);
			break;
		case ILike.OPERATOR:
			ILike ilik=new ILike(cb,entity);
			res=ilik.getCriteria(field, values);
			break;
		case GreaterThan.OPERATOR:
			GreaterThan gt = new GreaterThan(cb,entity);
			res=gt.getCriteria(field, values);
			break;
		case GreaterThanOrNull.OPERATOR:
			GreaterThanOrNull gtOrNull = new GreaterThanOrNull(cb,entity);
			res=gtOrNull.getCriteria(field, values);
			break;
		case GreaterThanEqual.OPERATOR:
			GreaterThanEqual gte = new GreaterThanEqual(cb,entity);
			res=gte.getCriteria(field, values);
			break;
		case LessThan.OPERATOR:
			LessThan lt = new LessThan(cb,entity);
			res=lt.getCriteria(field, values);
			break;
		case LessThanEqual.OPERATOR:
			LessThanEqual lte = new LessThanEqual(cb,entity);
			res=lte.getCriteria(field, values);
			break;
		case In.OPERATOR:
			In in = new In(cb,entity);
			res=in.getCriteria(field, values);
			break;
		}
		return res;
	}

	private Object[] convertValues(Class<?> targetType, Object[] values) {
		Object[] converted = new Object[values.length];
		for (int i = 0; i < values.length; i++) {
			converted[i] = convertValue(targetType, values[i]);
		}
		return converted;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private Object convertValue(Class<?> targetType, Object value) {
		if (value == null || targetType.isInstance(value)) return value;
		if (targetType.isEnum() && value instanceof String text) {
			try {
				return Enum.valueOf((Class<? extends Enum>) targetType, text.toUpperCase());
			} catch (IllegalArgumentException exception) {
				throw new IllegalArgumentException("Valore filtro non valido: " + text);
			}
		}
		if (targetType == LocalDate.class && value instanceof String text) {
			try {
				return LocalDate.parse(text);
			} catch (java.time.format.DateTimeParseException exception) {
				throw new IllegalArgumentException("Data filtro non valida: " + text);
			}
		}
		if (value instanceof Number number) {
			if (targetType == Long.class || targetType == long.class) return number.longValue();
			if (targetType == Integer.class || targetType == int.class) return number.intValue();
		}
		return value;
	}

}
