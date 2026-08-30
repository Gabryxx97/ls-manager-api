package it.lsimpresa.manager.utils.search.operators;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class ILike extends SqlOperator{

	public static final String OPERATOR="ILIKE";

	public ILike(CriteriaBuilder builder, Root<?> entity) {
		super(builder, entity, ILike.OPERATOR);
	}

	@Override
	public Predicate getCriteria(
			String fieldName, 
			Object... value) {
		
		String val=(String) value[0];
		return builder.like(
	            builder.lower(
	                entity.get(fieldName)
	            ), "%" + val.toLowerCase()+ "%"
	        );
	}
	
}
