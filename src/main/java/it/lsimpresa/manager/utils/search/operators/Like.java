package it.lsimpresa.manager.utils.search.operators;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class Like extends SqlOperator{

	public static final String OPERATOR="LIKE";
	
	public Like(CriteriaBuilder builder,Root<?> entity) {
		super(builder, entity,Like.OPERATOR);
	}
	
	@Override
	public Predicate getCriteria(String fieldName, Object... value) {
		return builder.like(entity.get(fieldName), (String)value[0]);
	}
	
}
