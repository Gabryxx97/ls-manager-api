package it.lsimpresa.manager.utils.search.operators;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class GreaterThanEqual extends SqlOperator {
	
	public static final String OPERATOR="GTE";
	
	public GreaterThanEqual(CriteriaBuilder builder, Root<?> entity) {
		super(builder, entity,GreaterThanEqual.OPERATOR);
	}
		
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Predicate getCriteria(String fieldName, Object... value) {
		Comparable comparable=(Comparable)value[0];
		return builder.greaterThanOrEqualTo(entity.get(fieldName), comparable);
	}

}
