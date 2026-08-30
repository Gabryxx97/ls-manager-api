package it.lsimpresa.manager.utils.search.operators;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class LessThan extends SqlOperator {
	
	public static final String OPERATOR="LT";

	public LessThan(CriteriaBuilder builder, Root<?> entity) {
		super(builder, entity, LessThan.OPERATOR);
	}

	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Predicate getCriteria(String fieldName, Object... value) {
		Comparable comparable=(Comparable)value[0];
		return builder.lessThan(entity.get(fieldName), comparable);
	}
}
